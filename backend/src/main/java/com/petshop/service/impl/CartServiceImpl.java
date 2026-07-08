package com.petshop.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petshop.dto.request.CartItemRequest;
import com.petshop.dto.response.CartDTO;
import com.petshop.dto.response.CartItemDTO;
import com.petshop.entity.Product;
import com.petshop.entity.ProductVariant;
import com.petshop.entity.User;
import com.petshop.exception.BadRequestException;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.ProductVariantRepository;
import com.petshop.repository.UserRepository;
import com.petshop.security.UserPrincipal;
import com.petshop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final Duration CART_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public CartDTO getCart() {
        User user = getCurrentUser();
        Map<String, Object> cartItems = getCartMap(user.getId());
        return buildCartDTO(cartItems);
    }

    @Override
    public CartDTO addToCart(CartItemRequest request) {
        User user = getCurrentUser();
        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        if (!variant.isActive()) {
            throw new BadRequestException("Sản phẩm không còn hoạt động");
        }

        if (variant.getStock() < request.getQuantity()) {
            throw new BadRequestException("Không đủ hàng trong kho");
        }

        Map<String, Object> cartItems = getCartMap(user.getId());
        String variantKey = variant.getId().toString();
        Map<String, Object> existingItem = (Map<String, Object>) cartItems.get(variantKey);

        int newQuantity = request.getQuantity();
        if (existingItem != null) {
            Integer currentQuantity = (Integer) existingItem.get("quantity");
            newQuantity = currentQuantity + request.getQuantity();
            if (variant.getStock() < newQuantity) {
                throw new BadRequestException("Không đủ hàng trong kho");
            }
        }

        Map<String, Object> itemData = new LinkedHashMap<>();
        itemData.put("variantId", variant.getId());
        itemData.put("quantity", newQuantity);
        itemData.put("createdAt", LocalDateTime.now().toString());
        cartItems.put(variantKey, itemData);

        saveCartMap(user.getId(), cartItems);
        return buildCartDTO(cartItems);
    }

    @Override
    public CartDTO updateCartItem(Long variantId, Integer quantity) {
        User user = getCurrentUser();
        if (quantity <= 0) {
            return removeFromCart(variantId);
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        if (variant.getStock() < quantity) {
            throw new BadRequestException("Không đủ hàng trong kho");
        }

        Map<String, Object> cartItems = getCartMap(user.getId());
        if (!cartItems.containsKey(variantId.toString())) {
            throw new BadRequestException("Sản phẩm không có trong giỏ hàng");
        }

        Map<String, Object> itemData = (Map<String, Object>) cartItems.get(variantId.toString());
        itemData.put("quantity", quantity);
        cartItems.put(variantId.toString(), itemData);
        saveCartMap(user.getId(), cartItems);
        return buildCartDTO(cartItems);
    }

    @Override
    public CartDTO removeFromCart(Long variantId) {
        User user = getCurrentUser();
        Map<String, Object> cartItems = getCartMap(user.getId());
        cartItems.remove(variantId.toString());
        saveCartMap(user.getId(), cartItems);
        return buildCartDTO(cartItems);
    }

    @Override
    public void clearCart() {
        User user = getCurrentUser();
        redisTemplate.delete(getCartKey(user.getId()));
    }

    @Override
    public Long countCartItems() {
        User user = getCurrentUser();
        Map<String, Object> cartItems = getCartMap(user.getId());
        return cartItems.values().stream()
                .mapToLong(item -> ((Number) ((Map<String, Object>) item).get("quantity")).longValue())
                .sum();
    }

    private Map<String, Object> getCartMap(Long userId) {
        Object raw = redisTemplate.opsForValue().get(getCartKey(userId));
        if (raw == null) {
            return new LinkedHashMap<>();
        }

        if (raw instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }

        try {
            return objectMapper.convertValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private void saveCartMap(Long userId, Map<String, Object> cartItems) {
        redisTemplate.opsForValue().set(getCartKey(userId), cartItems, CART_TTL.toMillis(), TimeUnit.MILLISECONDS);
    }

    private String getCartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private CartDTO buildCartDTO(Map<String, Object> cartItems) {
        List<CartItemDTO> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItems = 0;

        for (Map.Entry<String, Object> entry : cartItems.entrySet()) {
            Map<String, Object> itemData = (Map<String, Object>) entry.getValue();
            Long variantId = Long.valueOf(entry.getKey());
            Integer quantity = ((Number) itemData.get("quantity")).intValue();
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

            Product product = variant.getProduct();
            BigDecimal price = variant.getPrice();
            BigDecimal salePrice = product.getSalePrice();
            BigDecimal currentPrice = (salePrice != null && salePrice.compareTo(price) < 0) ? salePrice : price;
            BigDecimal lineTotal = currentPrice.multiply(BigDecimal.valueOf(quantity));

            subtotal = subtotal.add(lineTotal);
            totalItems += quantity;

            String productImage = null;
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                productImage = product.getImages().stream()
                        .filter(img -> img.isPrimary())
                        .findFirst()
                        .map(img -> img.getImageUrl())
                        .orElse(product.getImages().get(0).getImageUrl());
            }

            CartItemDTO item = CartItemDTO.builder()
                    .id(variantId)
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSlug(product.getSlug())
                    .productImage(productImage)
                    .variantId(variant.getId())
                    .variantName(variant.getName())
                    .price(price)
                    .salePrice(salePrice)
                    .currentPrice(currentPrice)
                    .stockQuantity(variant.getStock())
                    .inStock(variant.getStock() > 0)
                    .quantity(quantity)
                    .subtotal(lineTotal)
                    .createdAt(LocalDateTime.parse((String) itemData.getOrDefault("createdAt", LocalDateTime.now().toString())))
                    .build();
            items.add(item);
        }

        return CartDTO.builder()
                .items(items)
                .subtotal(subtotal)
                .totalItems(totalItems)
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Chưa đăng nhập");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

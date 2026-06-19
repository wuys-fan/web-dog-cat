package com.petshop.controller;

import com.petshop.dto.response.ProductDTO;
import com.petshop.entity.Product;
import com.petshop.entity.User;
import com.petshop.entity.Wishlist;
import com.petshop.exception.BadRequestException;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.ProductRepository;
import com.petshop.repository.UserRepository;
import com.petshop.repository.WishlistRepository;
import com.petshop.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getWishlist() {
        User user = getCurrentUser();
        List<Wishlist> wishlistItems = wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        List<ProductDTO> products = wishlistItems.stream()
            .map(item -> mapProductToDTO(item.getProduct()))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(products);
    }

    @PostMapping("/{productId}")
    @Transactional
    public ResponseEntity<Map<String, String>> addToWishlist(@PathVariable Long productId) {
        User user = getCurrentUser();
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
        
        if (wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            return ResponseEntity.ok(Map.of("message", "Sản phẩm đã có trong danh sách yêu thích"));
        }
        
        Wishlist wishlist = Wishlist.builder()
            .user(user)
            .product(product)
            .build();
        
        wishlistRepository.save(wishlist);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("message", "Đã thêm vào danh sách yêu thích"));
    }

    @DeleteMapping("/{productId}")
    @Transactional
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long productId) {
        User user = getCurrentUser();
        
        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(user.getId(), productId)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không có trong danh sách yêu thích"));
        
        wishlistRepository.delete(wishlist);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> checkInWishlist(@PathVariable Long productId) {
        User user = getCurrentUser();
        boolean exists = wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
        return ResponseEntity.ok(Map.of("inWishlist", exists));
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

    private ProductDTO mapProductToDTO(Product product) {
        BigDecimal minPrice = product.getVariants() != null ? product.getVariants().stream()
            .filter(v -> v.isActive())
            .map(v -> v.getPrice())
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO) : BigDecimal.ZERO;

        BigDecimal maxPrice = product.getVariants() != null ? product.getVariants().stream()
            .filter(v -> v.isActive())
            .map(v -> v.getPrice())
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO) : BigDecimal.ZERO;

        String primaryImage = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            primaryImage = product.getImages().stream()
                .filter(img -> img.isPrimary())
                .findFirst()
                .map(img -> img.getImageUrl())
                .orElse(product.getImages().get(0).getImageUrl());
        }

        int totalStock = product.getVariants() != null ? product.getVariants().stream()
            .filter(v -> v.isActive())
            .mapToInt(v -> v.getStock())
            .sum() : 0;

        return ProductDTO.builder()
            .id(product.getId())
            .name(product.getName())
            .slug(product.getSlug())
            .description(product.getDescription())
            .shortDescription(product.getShortDescription())
            .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
            .petType(product.getCategory() != null ? product.getCategory().getPetType() : null)
            .primaryImage(primaryImage)
            .basePrice(product.getBasePrice())
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .averageRating(product.getAverageRating())
            .reviewCount(product.getReviewCount())
            .soldCount(product.getSoldCount())
            .totalStock(totalStock)
            .featured(product.isFeatured())
            .active(product.isActive())
            .inStock(totalStock > 0)
            .createdAt(product.getCreatedAt())
            .build();
    }
}

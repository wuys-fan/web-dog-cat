package com.petshop.service;

import com.petshop.dto.request.CartItemRequest;
import com.petshop.dto.response.CartDTO;
import com.petshop.dto.response.CartItemDTO;

public interface CartService {

    CartDTO getCart();

    CartDTO addToCart(CartItemRequest request);

    CartDTO updateCartItem(Long variantId, Integer quantity);

    CartDTO removeFromCart(Long variantId);

    void clearCart();

    Long countCartItems();
}

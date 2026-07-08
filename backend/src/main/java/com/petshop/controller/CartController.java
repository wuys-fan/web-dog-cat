package com.petshop.controller;

import com.petshop.dto.request.CartItemRequest;
import com.petshop.dto.response.CartDTO;
import com.petshop.dto.response.CartItemDTO;
import com.petshop.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartDTO> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @PostMapping
    public ResponseEntity<CartDTO> addToCart(@Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addToCart(request));
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<CartDTO> updateCartItem(@PathVariable Long variantId,
                                                  @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateCartItem(variantId, quantity));
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<CartDTO> removeFromCart(@PathVariable Long variantId) {
        return ResponseEntity.ok(cartService.removeFromCart(variantId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }
}

package com.petshop.controller;

import com.petshop.dto.response.UserDTO;
import com.petshop.entity.User;
import com.petshop.repository.UserRepository;
import com.petshop.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers(@RequestParam(required = false) String role) {
        List<User> users = userRepository.findAll();
        if (role != null && !role.isEmpty()) {
            try {
                User.Role targetRole = User.Role.valueOf(role.toUpperCase());
                users = users.stream()
                        .filter(u -> u.getRole() == targetRole)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Ignore invalid role filter
            }
        }
        
        List<UserDTO> dtos = users.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(mapToDTO(user));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<UserDTO> updateStatus(@PathVariable Long id, @RequestParam Boolean status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(status);
        userRepository.save(user);
        return ResponseEntity.ok(mapToDTO(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getEmail())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatarUrl(user.getAvatar())
                .role(user.getRole())
                .active(user.isActive())
                .petCount(user.getPets() != null ? user.getPets().size() : 0)
                .orderCount(user.getOrders() != null ? user.getOrders().size() : 0)
                .createdAt(user.getCreatedAt())
                .build();
    }
}

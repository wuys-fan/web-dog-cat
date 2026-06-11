package com.petshop.service.impl;

import com.petshop.dto.response.UserDTO;
import com.petshop.entity.Order;
import com.petshop.entity.Pet;
import com.petshop.entity.User;
import com.petshop.exception.ResourceNotFoundException;
import com.petshop.repository.UserRepository;
import com.petshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = getUserEntityById(id);
        return convertToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, String fullName, String phone, String address) {
        User user = getUserEntityById(id);
        
        if (fullName != null && !fullName.isEmpty()) {
            user.setFullName(fullName);
        }
        if (phone != null && !phone.isEmpty()) {
            user.setPhone(phone);
        }
        if (address != null && !address.isEmpty()) {
            user.setAddress(address);
        }
        
        User updated = userRepository.save(user);
        return convertToDTO(updated);
    }

    @Override
    @Transactional
    public UserDTO updateUserStatus(Long id, boolean active) {
        User user = getUserEntityById(id);
        user.setActive(active);
        User updated = userRepository.save(user);
        return convertToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserEntityById(id);
        userRepository.delete(user);
    }

    @Override
    public User getUserEntityById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private UserDTO convertToDTO(User user) {
        int petCount = user.getPets() != null ? user.getPets().size() : 0;
        int orderCount = user.getOrders() != null ? user.getOrders().size() : 0;
        
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
            .petCount(petCount)
            .orderCount(orderCount)
            .createdAt(user.getCreatedAt())
            .build();
    }
}

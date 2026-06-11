package com.petshop.service;

import com.petshop.dto.response.UserDTO;
import com.petshop.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    
    // === Admin endpoints ===
    Page<UserDTO> getAllUsers(Pageable pageable);
    
    UserDTO getUserById(Long id);
    
    UserDTO updateUser(Long id, String fullName, String phone, String address);
    
    UserDTO updateUserStatus(Long id, boolean active);
    
    void deleteUser(Long id);
    
    // === Internal methods ===
    User getUserEntityById(Long id);
    
    User getUserEntityByEmail(String email);
}

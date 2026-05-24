package com.example.restaurantmanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.restaurantmanagement.dto.AuthResponse;
import com.example.restaurantmanagement.dto.LoginRequest;
import com.example.restaurantmanagement.dto.RegisterRequest;
import com.example.restaurantmanagement.entity.User;

public interface UserService extends IService<User> {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    User createStaff(RegisterRequest request, String operatorRole);
}

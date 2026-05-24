package com.example.restaurantmanagement.controller;

import com.example.restaurantmanagement.auth.RequireRoles;
import com.example.restaurantmanagement.auth.RoleInterceptor;
import com.example.restaurantmanagement.common.ApiResponse;
import com.example.restaurantmanagement.dto.AuthResponse;
import com.example.restaurantmanagement.dto.LoginRequest;
import com.example.restaurantmanagement.dto.RegisterRequest;
import com.example.restaurantmanagement.entity.User;
import com.example.restaurantmanagement.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    @PostMapping("/staff")
    @RequireRoles({"OWNER", "MANAGER"})
    public ApiResponse<User> createStaff(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpServletRequest) {
        String operatorRole = (String) httpServletRequest.getAttribute(RoleInterceptor.CURRENT_USER_ROLE_ATTR);
        return ApiResponse.success(userService.createStaff(request, operatorRole));
    }
}

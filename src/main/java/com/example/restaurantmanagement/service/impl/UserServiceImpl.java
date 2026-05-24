package com.example.restaurantmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.restaurantmanagement.auth.AuthSessionManager;
import com.example.restaurantmanagement.dto.AuthResponse;
import com.example.restaurantmanagement.dto.LoginRequest;
import com.example.restaurantmanagement.dto.RegisterRequest;
import com.example.restaurantmanagement.entity.User;
import com.example.restaurantmanagement.exception.BusinessException;
import com.example.restaurantmanagement.mapper.UserMapper;
import com.example.restaurantmanagement.service.UserService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final AuthSessionManager authSessionManager;

    public UserServiceImpl(AuthSessionManager authSessionManager) {
        this.authSessionManager = authSessionManager;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        User existed = lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .one();
        if (existed != null) {
            throw new BusinessException("用户名已存在");
        }
        String targetRole = normalizeRole(request.getRole());
        validateAdminPassword(request.getAdminPassword());
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(md5(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole(targetRole);
        user.setCreateTime(LocalDateTime.now());
        save(user);
        return toAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername());
        User user = getOne(queryWrapper);
        if (user == null || !user.getPassword().equals(md5(request.getPassword()))) {
            throw new BusinessException("用户名或密码错误");
        }
        return toAuthResponse(user);
    }

    @Override
    public User createStaff(RegisterRequest request, String operatorRole) {
        User existed = lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .one();
        if (existed != null) {
            throw new BusinessException("用户名已存在");
        }

        String targetRole = normalizeRole(request.getRole());
        if (!isRoleAssignable(operatorRole, targetRole)) {
            throw new BusinessException("当前账号无权创建该角色");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(md5(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole(targetRole);
        user.setCreateTime(LocalDateTime.now());
        save(user);
        user.setPassword(null);
        return user;
    }

    private String md5(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }

    private AuthResponse toAuthResponse(User user) {
        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRole(user.getRole());
        response.setCreateTime(user.getCreateTime());
        response.setToken(authSessionManager.createSession(user.getId()));
        return response;
    }

    private String normalizeRole(String role) {
        String targetRole = role == null || role.isBlank() ? "WAITER" : role.trim();
        if (!"OWNER".equals(targetRole)
                && !"MANAGER".equals(targetRole)
                && !"WAITER".equals(targetRole)
                && !"CASHIER".equals(targetRole)
                && !"CHEF".equals(targetRole)) {
            throw new BusinessException("角色类型无效");
        }
        return targetRole;
    }

    private void validateAdminPassword(String adminPassword) {
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new BusinessException("请输入管理员密码");
        }

        User adminUser = lambdaQuery()
                .eq(User::getUsername, "admin")
                .one();
        if (adminUser == null || !md5(adminPassword).equals(adminUser.getPassword())) {
            throw new BusinessException("管理员密码错误");
        }
    }

    private boolean isRoleAssignable(String operatorRole, String targetRole) {
        if ("OWNER".equals(operatorRole)) {
            return "OWNER".equals(targetRole)
                    || "MANAGER".equals(targetRole)
                    || "WAITER".equals(targetRole)
                    || "CASHIER".equals(targetRole)
                    || "CHEF".equals(targetRole);
        }
        if ("MANAGER".equals(operatorRole)) {
            return "WAITER".equals(targetRole)
                    || "CASHIER".equals(targetRole)
                    || "CHEF".equals(targetRole);
        }
        return false;
    }
}

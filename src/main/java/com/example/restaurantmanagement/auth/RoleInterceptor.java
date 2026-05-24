package com.example.restaurantmanagement.auth;

import com.example.restaurantmanagement.entity.User;
import com.example.restaurantmanagement.exception.BusinessException;
import com.example.restaurantmanagement.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ID_ATTR = "currentUserId";
    public static final String CURRENT_USER_ROLE_ATTR = "currentUserRole";

    private final AuthSessionManager authSessionManager;
    private final UserMapper userMapper;

    public RoleInterceptor(AuthSessionManager authSessionManager, UserMapper userMapper) {
        this.authSessionManager = authSessionManager;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRoles requireRoles = handlerMethod.getMethodAnnotation(RequireRoles.class);
        if (requireRoles == null) {
            requireRoles = handlerMethod.getBeanType().getAnnotation(RequireRoles.class);
        }
        if (requireRoles == null) {
            return true;
        }

        String token = request.getHeader(AuthSessionManager.TOKEN_HEADER);
        AuthSessionManager.SessionInfo session = authSessionManager.getSession(token);
        if (session == null || session.userId() == null) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }

        User currentUser = userMapper.selectById(session.userId());
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole().isBlank()) {
            throw new BusinessException("当前登录用户不存在或角色无效");
        }

        String currentRole = currentUser.getRole().trim();
        request.setAttribute(CURRENT_USER_ID_ATTR, currentUser.getId());
        request.setAttribute(CURRENT_USER_ROLE_ATTR, currentRole);
        if ("OWNER".equals(currentRole)) {
            return true;
        }

        Set<String> allowedRoles = new LinkedHashSet<>(Arrays.asList(requireRoles.value()));
        if (!allowedRoles.contains(currentRole)) {
            throw new BusinessException("当前角色无权执行此操作");
        }
        return true;
    }
}

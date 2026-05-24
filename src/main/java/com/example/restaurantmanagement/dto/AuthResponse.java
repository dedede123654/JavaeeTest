package com.example.restaurantmanagement.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AuthResponse {

    private Integer id;

    private String username;

    private String nickname;

    private String role;

    private String token;

    private LocalDateTime createTime;
}

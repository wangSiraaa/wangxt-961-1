package com.charging.shed.controller;

import com.charging.shed.dto.ApiResponse;
import com.charging.shed.dto.LoginDTO;
import com.charging.shed.entity.User;
import com.charging.shed.service.UserService;
import com.charging.shed.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        Map<String, Object> result = userService.login(loginDTO);
        return ApiResponse.success(result);
    }

    @GetMapping("/info")
    public ApiResponse<User> getCurrentUser(@RequestAttribute("userId") Long userId) {
        User user = userService.getUserById(userId);
        user.setPassword(null);
        return ApiResponse.success(user);
    }
}

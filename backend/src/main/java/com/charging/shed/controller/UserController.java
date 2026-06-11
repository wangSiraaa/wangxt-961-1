package com.charging.shed.controller;

import com.charging.shed.dto.ApiResponse;
import com.charging.shed.dto.VerifyDTO;
import com.charging.shed.entity.User;
import com.charging.shed.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/verify")
    public ApiResponse<User> verifyIdentity(@RequestAttribute("userId") Long userId,
                                            @Valid @RequestBody VerifyDTO verifyDTO) {
        User user = userService.verifyIdentity(userId, verifyDTO.getRealName(),
                verifyDTO.getIdCard(), verifyDTO.getPhone());
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    @GetMapping("/balance")
    public ApiResponse<User> getBalance(@RequestAttribute("userId") Long userId) {
        User user = userService.getUserById(userId);
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    @PostMapping("/recharge")
    public ApiResponse<User> recharge(@RequestAttribute("userId") Long userId,
                                      @RequestParam("amount") java.math.BigDecimal amount) {
        User user = userService.recharge(userId, amount);
        user.setPassword(null);
        return ApiResponse.success(user);
    }
}

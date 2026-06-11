package com.charging.shed.service;

import com.charging.shed.dto.LoginDTO;
import com.charging.shed.entity.User;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.repository.UserRepository;
import com.charging.shed.util.JwtUtil;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    public Map<String, Object> login(LoginDTO loginDTO) {
        User user = userRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "密码错误");
        }

        if (!User.Status.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(403, "账户已被冻结");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        result.put("isVerified", user.getVerified());
        result.put("balance", user.getBalance());

        return result;
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public User getUserById(Long id) {
        return getById(id);
    }

    @Transactional
    public User recharge(Long userId, BigDecimal amount) {
        User user = getById(userId);
        user.setBalance(user.getBalance().add(amount));
        return userRepository.save(user);
    }

    public List<User> getByRole(String role) {
        return userRepository.findByRole(role);
    }

    @Transactional
    public User verifyIdentity(Long userId, String realName, String idCard, String phone) {
        User user = getById(userId);

        if (user.getVerified()) {
            throw new BusinessException("用户已实名认证");
        }

        if (userRepository.existsByPhone(phone) && !user.getPhone().equals(phone)) {
            throw new BusinessException("该手机号已被绑定");
        }

        user.setRealName(realName);
        user.setIdCard(idCard);
        user.setPhone(phone);
        user.setVerified(true);

        return userRepository.save(user);
    }

    @Transactional
    public User updateBalance(Long userId, BigDecimal amount) {
        User user = getById(userId);
        user.setBalance(user.getBalance().add(amount));
        return userRepository.save(user);
    }

    public boolean hasUnpaidBills(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getBalance().compareTo(BigDecimal.ZERO) < 0)
                .orElse(false);
    }

    public BigDecimal getUnpaidAmount(Long userId) {
        User user = getById(userId);
        BigDecimal balance = user.getBalance();
        return balance.compareTo(BigDecimal.ZERO) < 0 ? balance.negate() : BigDecimal.ZERO;
    }
}

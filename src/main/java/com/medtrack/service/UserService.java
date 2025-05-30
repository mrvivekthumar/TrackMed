package com.medtrack.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.medtrack.dto.AuthResponse;
import com.medtrack.dto.UserRequestDto;
import com.medtrack.exceptions.AuthException;
import com.medtrack.mapper.UserMapper;
import com.medtrack.model.User;
import com.medtrack.repository.UserRepo;
import com.medtrack.security.JwtUtil;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class UserService {

    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    public User signUp(UserRequestDto userDto) {

        userRepo.findOneByEmail(userDto.email()).ifPresent(existingUser -> {
            throw new AuthException("User with Email %s already exists.".formatted(existingUser.getEmail()));
        });

        User user = userMapper.toEntity(userDto);
        user.setPassword(passwordEncoder.encode(userDto.password()));

        return userRepo.save(user);
    }

    public AuthResponse signIn(UserRequestDto userDto) {
        User existingUser = userRepo.findOneByEmail(userDto.email()).orElseThrow(
                () -> new EntityNotFoundException("User with email %s not found".formatted(userDto.email())));

        if (!passwordEncoder.matches(userDto.password(), existingUser.getPassword())) {
            throw new AuthException("Invalid Password");
        }

        String token = jwtUtil.generateToken(existingUser.getEmail());

        return new AuthResponse(userMapper.toDto(existingUser), token);
    }

    public User getUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with ID %d not found".formatted(userId)));
    }

    public void delete(Long id) {
        userRepo.deleteById(id);
    }

}

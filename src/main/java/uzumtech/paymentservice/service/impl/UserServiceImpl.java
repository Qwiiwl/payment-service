package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.dto.request.UserLoginRequest;
import uzumtech.paymentservice.dto.request.UserRegisterRequest;
import uzumtech.paymentservice.dto.response.UserResponse;
import uzumtech.paymentservice.entity.UserEntity;
import uzumtech.paymentservice.exception.InvalidCredentialsException;
import uzumtech.paymentservice.exception.UserAlreadyExistsException;
import uzumtech.paymentservice.repository.UserRepository;
import uzumtech.paymentservice.service.UserService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder; // ✅ теперь инжектится

    @Override
    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new UserAlreadyExistsException("User already exists");
        }

        UserEntity user = UserEntity.builder()
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .password(passwordEncoder.encode(request.password()))
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return new UserResponse(user.getId(), user.getFullName(), user.getPhoneNumber(), user.getCreatedAt());
    }

    @Override
    public UserResponse login(UserLoginRequest request) {
        UserEntity user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid phone or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid phone or password");
        }

        return new UserResponse(user.getId(), user.getFullName(), user.getPhoneNumber(), user.getCreatedAt());
    }
}
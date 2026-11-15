package com.konate.ithelpdesk.user.BusinessLayer;

import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.DataLayer.UserIdentifier;
import com.konate.ithelpdesk.user.DataLayer.UserRepository;
import com.konate.ithelpdesk.user.MappingLayer.UserRequestMapper;
import com.konate.ithelpdesk.user.MappingLayer.UserResponseMapper;
import com.konate.ithelpdesk.user.PresentationLayer.LoginResponse;
import com.konate.ithelpdesk.user.PresentationLayer.UserRequestModel;
import com.konate.ithelpdesk.user.PresentationLayer.UserResponseModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserRequestMapper userRequestMapper;
    private final UserResponseMapper userResponseMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // token -> user. Kept in memory, so everyone is logged out when the app restarts.
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    public AuthServiceImpl(UserRepository userRepository,
                           UserRequestMapper userRequestMapper,
                           UserResponseMapper userResponseMapper
                           ) {
        this.userRepository = userRepository;
        this.userRequestMapper = userRequestMapper;
        this.userResponseMapper = userResponseMapper;
    }

    @Override
    public UserResponseModel registerUser(UserRequestModel requestModel) {
        if (userRepository.findAllByEmail(requestModel.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used: " + requestModel.getEmail());
        }
        User user = userRequestMapper.toRequestModel(requestModel, new UserIdentifier());
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A password is required");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userResponseMapper.toResponseModel(userRepository.save(user));
    }

    @Override
    public List<UserResponseModel> findAllUsers() {
        return userResponseMapper.entityListToResponseModelList(userRepository.findAll());
    }

    @Override
    public Optional<LoginResponse> loginUser(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }
        return userRepository.findAllByEmail(email)
                .filter(user -> passwordMatches(user, password))
                .map(user -> {
                    String token = UUID.randomUUID().toString();
                    sessions.put(token, user);
                    return new LoginResponse(token, userResponseMapper.toResponseModel(user));
                });
    }

    /**
     * BCrypt for hashed passwords. Legacy plaintext values (seed data) are compared in constant time
     * and upgraded to a BCrypt hash after a successful login.
     */
    private boolean passwordMatches(User user, String password) {
        String stored = user.getPassword();
        if (stored == null) return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(password, stored);
        }
        boolean ok = MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8));
        if (ok) {
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }
        return ok;
    }

    @Override
    public Optional<User> findByToken(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(token));
    }

    @Override
    public Optional<UserResponseModel> getCurrentUser(String token) {
        return findByToken(token).map(userResponseMapper::toResponseModel);
    }

    @Override
    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
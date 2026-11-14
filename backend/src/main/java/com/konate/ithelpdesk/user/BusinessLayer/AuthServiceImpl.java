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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
//    private final PasswordEncoder passwordEncoder;

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
                .filter(user -> password.equals(user.getPassword()))
                .map(user -> {
                    String token = UUID.randomUUID().toString();
                    sessions.put(token, user);
                    return new LoginResponse(token, userResponseMapper.toResponseModel(user));
                });
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
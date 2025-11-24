package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.AuthRequest;
import com.ecommerce.userservice.dto.AuthResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.UserRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${keycloak.token-url}")
    private String keycloakTokenUrl;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(AuthRequest request) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "password");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("username", request.getUsername());
            formData.add("password", request.getPassword());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);

            ResponseEntity<java.util.Map> response = restTemplate.exchange(
                keycloakTokenUrl, 
                HttpMethod.POST, 
                entity, 
                java.util.Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                java.util.Map<?, ?> body = response.getBody();
                String accessToken = (String) body.get("access_token");
                String refreshToken = (String) body.get("refresh_token");
                
                // Extract role from user in database or token
                User user = userRepository.findByUsername(request.getUsername()).orElse(null);
                String role = user != null ? user.getRole().toString() : "USER";
                
                return new AuthResponse(accessToken, request.getUsername(), role);
            } else {
                throw new RuntimeException("Failed to obtain token from Keycloak");
            }
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    public User register(User user, String rawPassword) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User savedUser = userRepository.save(user);

        // Create user in Keycloak using REST API
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Get admin token
            HttpHeaders adminHeaders = new HttpHeaders();
            adminHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> adminForm = new LinkedMultiValueMap<>();
            adminForm.add("grant_type", "password");
            adminForm.add("client_id", "admin-cli");
            adminForm.add("username", adminUsername);
            adminForm.add("password", adminPassword);

            HttpEntity<MultiValueMap<String, String>> adminEntity = new HttpEntity<>(adminForm, adminHeaders);
            ResponseEntity<Map> adminResponse = restTemplate.exchange(
                serverUrl + "/realms/master/protocol/openid-connect/token",
                HttpMethod.POST,
                adminEntity,
                Map.class
            );

            String adminToken = (String) adminResponse.getBody().get("access_token");

            // Create user
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setContentType(MediaType.APPLICATION_JSON);
            userHeaders.setBearerAuth(adminToken);

            Map<String, Object> userData = new HashMap<>();
            userData.put("username", savedUser.getUsername());
            userData.put("email", savedUser.getEmail());
            userData.put("firstName", savedUser.getFirstName());
            userData.put("lastName", savedUser.getLastName());
            userData.put("enabled", true);
            userData.put("credentials", Arrays.asList(Map.of(
                "type", "password",
                "value", rawPassword, // raw password
                "temporary", false
            )));

            HttpEntity<Map<String, Object>> userEntity = new HttpEntity<>(userData, userHeaders);
            ResponseEntity<String> createResponse = restTemplate.exchange(
                serverUrl + "/admin/realms/" + realm + "/users",
                HttpMethod.POST,
                userEntity,
                String.class
            );

            // Get user ID from location header
            String location = createResponse.getHeaders().getLocation().toString();
            String userId = location.substring(location.lastIndexOf('/') + 1);

            // Assign role
            Map<String, Object> roleData = new HashMap<>();
            roleData.put("name", savedUser.getRole().name().toLowerCase());

            HttpEntity<java.util.List<Map<String, Object>>> roleEntity = new HttpEntity<>(Arrays.asList(roleData), userHeaders);
            restTemplate.exchange(
                serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm",
                HttpMethod.POST,
                roleEntity,
                String.class
            );

        } catch (Exception e) {
            // If Keycloak fails, delete from DB
            userRepository.delete(savedUser);
            throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage(), e);
        }

        return savedUser;
    }

    public void createAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", "Admin", "User", "admin@gmail.com", "1234567890", "Admin Address");
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
        }
    }
}

package com.ecommerce.productservice.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final RestTemplate restTemplate;

    public CustomUserDetailsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Call user-service to get user details
        String userServiceUrl = "http://user-service/api/users/details/" + username;
        try {
            Map<String, Object> userDetails = restTemplate.getForObject(userServiceUrl, Map.class);
            if (userDetails == null) {
                throw new UsernameNotFoundException("User not found: " + username);
            }
            String role = (String) userDetails.get("role");
            return new org.springframework.security.core.userdetails.User(
                    username,
                    "", // Password not needed for JWT validation
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );
        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
    }
}

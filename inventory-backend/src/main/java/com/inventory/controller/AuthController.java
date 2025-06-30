package com.inventory.controller;

import com.inventory.model.User;
import com.inventory.model.User.Role; // Import User.Role enum
import com.inventory.repository.UserRepository;
import com.inventory.service.UserDetailsServiceImpl;
import com.inventory.util.JwtUtil;
import jakarta.annotation.PostConstruct; // Import for @PostConstruct
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections; // For Collections.singleton
import java.util.HashMap;
import java.util.List; // For List
import java.util.Map;
import java.util.Set; // For Set<Role>
import java.util.stream.Collectors; // For Collectors

/**
 * REST Controller for user authentication.
 * Handles user login and token generation.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS}) // Only allow POST for auth
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository; // To create initial user

    /**
     * Initializes the application with a default OWNER user if no users exist.
     * This is for development convenience. In production, users would be created via an admin interface.
     */
    @PostConstruct
    public void init() {
        if (userRepository.count() == 0) {
            User owner = new User("owner", passwordEncoder.encode("password"), Set.of(Role.OWNER));
            userRepository.save(owner);
            System.out.println("Created default OWNER user: username='owner', password='password'");

            User cashier = new User("cashier", passwordEncoder.encode("password"), Set.of(Role.CASHIER));
            userRepository.save(cashier);
            System.out.println("Created default CASHIER user: username='cashier', password='password'");
        }
    }


    /**
     * Handles user login requests.
     * @param authenticationRequest A map containing "username" and "password".
     * @return ResponseEntity with JWT token and user roles on success, or error message on failure.
     */
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody Map<String, String> authenticationRequest) {
        String username = authenticationRequest.get("username");
        String password = authenticationRequest.get("password");

        try {
            // Authenticate user credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect username or password");
        }

        // If authentication succeeds, load user details and generate JWT
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        final String jwt = jwtUtil.generateToken(userDetails);

        // Get roles and send them back to the frontend for UI adaptation
        // IMPORTANT: Ensure "ROLE_" prefix is included as expected by frontend's ApiClient/Spring Security
        List<String> rolesWithPrefix = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority()) // These authorities already have "ROLE_" prefix from UserDetailsServiceImpl
                .collect(Collectors.toList());


        Map<String, Object> response = new HashMap<>();
        response.put("jwt", jwt);
        response.put("username", userDetails.getUsername());
        response.put("roles", rolesWithPrefix); // Send roles with "ROLE_" prefix

        // --- NEW DEBUGGING LOGGING ---
        System.out.println("--- DEBUG (Backend AuthController): Sending Login Response ---");
        System.out.println("Login Response Map: " + response);
        System.out.println("Roles being sent: " + response.get("roles"));
        System.out.println("--- END DEBUG (Backend AuthController) ---");

        return ResponseEntity.ok(response);
    }
}

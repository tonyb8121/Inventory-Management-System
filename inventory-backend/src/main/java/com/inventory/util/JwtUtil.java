package com.inventory.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails; // Keep for generateToken
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for JSON Web Token (JWT) operations.
 * Handles generation, parsing, and validation of JWTs.
 */
@Component
public class JwtUtil {

    // Secret key for signing the JWTs. Loaded from application.properties.
    @Value("${jwt.secret}")
    private String secretString; // This will hold the base66 encoded string from properties

    private SecretKey secretKey;

    // Initialize the secret key from the base64 encoded string
    private SecretKey getSigningKey() {
        if (secretKey == null) {
            // Ensure the secret key is long enough for HS512 (at least 64 bytes or 512 bits)
            // If the provided secret string is too short, generate a secure one.
            if (secretString == null || secretString.length() < 64) { // 64 chars is roughly 64 bytes for base64
                secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512); // Generate a strong key
                System.out.println("WARNING: JWT secret not securely configured or too short. Generating new key. " +
                        "Please set 'jwt.secret' in application.properties with a strong Base64 encoded key: " +
                        java.util.Base64.getEncoder().encodeToString(secretKey.getEncoded()));
            } else {
                secretKey = Keys.hmacShaKeyFor(secretString.getBytes());
            }
        }
        return secretKey;
    }

    // Token validity in milliseconds (e.g., 1 hour)
    @Value("${jwt.expiration}")
    private long expiration; // Loaded from application.properties

    /**
     * Extracts the username (subject) from a JWT.
     * @param token The JWT string.
     * @return The username.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a JWT.
     * @param token The JWT string.
     * @return The expiration date.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the JWT's body.
     * @param token The JWT string.
     * @param claimsResolver Function to resolve the desired claim.
     * @param <T> The type of the claim.
     * @return The extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from the JWT's body.
     * Made public for direct access from JwtAuthenticationFilter.
     * @param token The JWT string.
     * @return All claims.
     */
    public Claims extractAllClaims(String token) { // MODIFIED: Made public
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Checks if the token has expired.
     * @param token The JWT string.
     * @return true if the token is expired, false otherwise.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Generates a JWT for a given UserDetails.
     * @param userDetails The UserDetails object.
     * @return The generated JWT string.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Add roles as claims. These will be "ROLE_OWNER", "ROLE_CASHIER" etc.
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // Get the string representation (e.g., "ROLE_OWNER")
                .collect(Collectors.toList()));

        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Creates the JWT.
     * @param claims Custom claims for the token.
     * @param subject The subject of the token (typically username).
     * @return The JWT string.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validates a JWT.
     * In a stateless system, this primarily checks if the token is expired and well-formed.
     * The username check is often handled by extracting and setting the context.
     * @param token The JWT string.
     * @return true if the token is valid (not expired and parsable), false otherwise.
     */
    public Boolean validateToken(String token) { // MODIFIED: Simplified to only take token
        try {
            extractAllClaims(token); // Attempt to parse to check signature and validity
            return !isTokenExpired(token); // Check if expired
        } catch (Exception e) {
            System.err.println("Token validation failed during parsing/expiration check: " + e.getMessage());
            return false;
        }
    }
}

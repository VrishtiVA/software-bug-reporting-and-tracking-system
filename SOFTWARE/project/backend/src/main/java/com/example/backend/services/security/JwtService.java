package com.example.backend.services.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * The contents of this service were influenced by:
 * <ul>
 *     <li> <a href="https://www.geeksforgeeks.org/reactjs/spring-security-login-page-with-react/">Spring Security Login Page with React</a> </li>
 *     <li> https://dev.to/realnamehidden1_61/how-to-use-jwt-authentication-in-spring-boot-java-21-an-end-to-end-beginner-guide-3pc4 </li>
 * </ul>
 */
@Service
public class JwtService {
    
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Autowired
    private UserDetailsService userDetailsService;

    //Using JWT secret from application.properties
    @Value("${jwt.secret}")
    private String jwtSecret;

    //4 hours token expiration time
    private final long jwtExpirationMs = 14400000;

    /**
     * Wrap the JwtSecret in a SecretKey to use for signing tokens.
     * @return The secret key derived from JwtSecret
     */
    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a signed token for the authenticated user.
     * The token contains:
     * - Subject: the email
     * - Issued at: current timestamp
     * - Expiration: current timestamp + expiration time
     * @param authentication The authentication object of the logged-in user
     * @return A signed JWT string
     */
    public String generateToken(Authentication authentication) {

        //Generate JWT Token
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        String token = Jwts.builder()
            .setSubject(authentication.getName()) //The username
            .setIssuedAt(now) //Current time
            .setExpiration(expiryDate) //Expiration
            .signWith(getSigningKey(), SignatureAlgorithm.HS256) //Sign token with HMAC SHA-256 (HS256) algorithm
            .compact(); //Convert token into a string

        //Return JWT Token
        log.info("Generated JWT Token: {}", token);
        return token;
    }

    /**
     * Extract the subject from a given JWT.
     * @param token the string JWT
     * @return the subject/username stored in the token.
     */
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey()) //Use same key to verify token signature
            .build()
            .parseClaimsJws(token) //Validate signature, expiration and token structure
            .getBody()
            .getSubject();
    }

    /**
     * Method to filter the JWT token, figure out the user it belongs to and if they can be authenticated.
     * This method is usually used as part of the security filter chain, to handle the authentication header before handling with requests.
     * @param request The incoming request that contains the Authorisation header, that contains the JWT
     * @return The username of the authenticated user or null if invalid authentication.
     *         The return value of this method is often unused, but is useful particularly when needing
     *         to handle authentication while bypassing authentication by spring security e.g. for the notification stream.
     */
    public String tokenFilterLogic(HttpServletRequest request) {
        //Get the token
        String token = request.getHeader("Authorization");
//        log.info("JWT Token in JWTTokenValidationFilter found in header: {}", token);
        String username = null;

        if (token != null) {
            try {
                //Remove pre-appended 'Bearer ' from token.
                token = token.replace("Bearer ", "");

                //Fetch the username from the token
                username = extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                //Set authentication object in security context.
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Authenticated user: {}", username);

            } catch (Exception e) {
                //Clear security context since user cannot be authenticated.
                SecurityContextHolder.clearContext();
                log.warn("Authentication failed: User does not have a valid JWT Authentication Token. Issue: {}", e.getMessage());
            }
        } else {
            SecurityContextHolder.clearContext();
            log.debug("Authentication failed: User does not have a valid JWT Authentication Token.");
        }

        return username;
    }

}

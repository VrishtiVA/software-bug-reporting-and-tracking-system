package com.example.backend.configuration;

import com.example.backend.services.security.JwtValidationFilter;
import com.example.backend.services.security.UserDetailsServiceImplementation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security and CORs configuration.
 * The contents of this configuration class were influenced by:
 * <ul>
 *     <li> <a href="https://www.geeksforgeeks.org/reactjs/spring-security-login-page-with-react/">Spring Security Login Page with React</a> </li>
 * </ul>
 */
@Configuration
public class ApplicationConfiguration {

    @Autowired
    private UserDetailsServiceImplementation userDetailsService;

    /* --------------- Security Configuration --------------- */

    /** Spring Security Configuration */
    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        ObjectProvider<JwtValidationFilter> jwtFilterProvider
    ) throws Exception {

        //Disabling Cross Site Request Forgery, since using JWT Authentication Token.
        //The frontend will manually add the token to the Authorisation Header to requests, therefore
        //these protections aren't necessary.
        http.csrf(csrf -> csrf.disable());

        //Enable CORS support, using the CORS Configuration defined.
        http.cors(Customizer.withDefaults());

        //Stateless authentication with JWT
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        //Authorisation settings for endpoints
        http.authorizeHttpRequests(request -> request
            //Public endpoints
            .requestMatchers(
                "/api/auth/**",
                "/notifications/stream"
            ).permitAll()
            //All other endpoints require authentication
            .anyRequest().authenticated()
        );

        //Run JWT filter before checking authentication for endpoints
        jwtFilterProvider.ifAvailable(jwtFilter ->
            http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        );

        //Use custom user details service for authentication
        http.userDetailsService(userDetailsService);

        return http.build();
    }

    /** Password encode bean for encoding passwords securely, using BCryptPasswordEncoder. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /* --------------- CORS Configuration --------------- */

    /**
     * Cross Origin Resource Sharing configuration, defining how this should behave.
     * Needed due to external frontend.
     * Useful resource: <a href="https://www.baeldung.com/spring-cors">Spring CORS</a>
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        //Only requests from frontend allowed for now.
        corsConfiguration.addAllowedOrigin("http://localhost:5173");
        //Allow all different method requests including GET, POST, PUT, DELETE, PATCH, OPTIONS requests.
        corsConfiguration.addAllowedMethod("*");
        //Allow browser to credentials e.g. Authorisation tokens in header.
        corsConfiguration.setAllowCredentials(true);
        //Allow frontend to send headers.
        corsConfiguration.addAllowedHeader("*");
        //Allow frontend JS to read these response headers.
        corsConfiguration.addExposedHeader("Authorization");
        corsConfiguration.addExposedHeader("Location");
        //Allow browser to remember permissions for 1 hour.
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

}

package com.info7255.demo.filter;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final String CLIENT_ID = "GET_YOUR_CLIENT_ID_FROM_GOOGLE";
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String idTokenString = extractIdToken(request);
        logger.info("ID token string: {}", idTokenString);

        try {
            if (idTokenString != null && !idTokenString.isEmpty() ) {
                GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new ApacheHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(Collections.singletonList(CLIENT_ID))
                        .build();
                GoogleIdToken idToken = verifier.verify(idTokenString);

                logger.info("idToken: {}", idToken);

                if (idToken != null) {
                    GoogleIdToken.Payload payload = idToken.getPayload();
                } else {
                    // Invalid ID token
                    System.out.println("Invalid ID token.");
                    logger.error("Invalid ID token: {}", idTokenString);
                    ResponseEntity<String> unauthorizedResponse = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID token.");
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write(unauthorizedResponse.getBody());
                    response.getWriter().flush();
                    return;
                }
            } else {
                ResponseEntity<String> unauthorizedResponse = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header missing.");
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write(unauthorizedResponse.getBody());
                response.getWriter().flush();
                return;
            }
        } catch (GeneralSecurityException e) {
            logger.error("Token verification failed: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);

    }


    private String extractIdToken(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        return null;
    }


}

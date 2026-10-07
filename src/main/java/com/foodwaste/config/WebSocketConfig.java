package com.foodwaste.config;

import com.foodwaste.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.support.ChannelInterceptor;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    public WebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry) {

        registry.enableSimpleBroker(
                "/topic",
                "/queue"
        );

        registry.setApplicationDestinationPrefixes("/app");

        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry) {

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {

        registration.interceptors(new ChannelInterceptor() {

            @Override
            public Message<?> preSend(
                    Message<?> message,
                    MessageChannel channel) {

                StompHeaderAccessor accessor =
                        StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                    String authHeader =
                            accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null &&
                            authHeader.startsWith("Bearer ")) {

                        String token = authHeader.substring(7);

                        try {
                            Claims claims = jwtUtil.parse(token);

                            String email = claims.getSubject();
                            String role = claims.get("role", String.class);

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            email,
                                            null,
                                            List.of(
                                                    new SimpleGrantedAuthority(
                                                            "ROLE_" + role
                                                    )
                                            )
                                    );

                            accessor.setUser(authentication);

                        } catch (JwtException | IllegalArgumentException e) {
                            throw new IllegalArgumentException(
                                    "Invalid or expired JWT"
                            );
                        }
                    }
                }

                return message;
            }
        });
    }
}
package com.tt.auth.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        String redirectUrl = request.getContextPath() + "/";

        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if ("ROLE_CUSTOMER".equals(auth.getAuthority())) {
                redirectUrl = "/customer/dashboard";
                break;
            } else if ("ROLE_OFFICER".equals(auth.getAuthority())) {
                redirectUrl = "/officer/dashboard";
                break;
            } else if ("ROLE_ADMIN".equals(auth.getAuthority())) {
                redirectUrl = "/admin/dashboard";
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}

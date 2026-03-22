package de.wissensdatenbank.config;

import java.io.IOException;
import java.util.regex.Pattern;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpaStaticResourceFilter extends OncePerRequestFilter {

    private static final Pattern STATIC_ASSET_EXT = Pattern.compile(".*\\.(js|css|ico|png|woff2|woff|ttf|svg|map|json)$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith("/api/")) {
            String withoutLeadingSlash = path.substring(1);
            int slashIndex = withoutLeadingSlash.indexOf('/');
            if (slashIndex > 0) {
                String remaining = withoutLeadingSlash.substring(slashIndex);
                if (STATIC_ASSET_EXT.matcher(remaining).matches()) {
                    request.getRequestDispatcher(remaining).forward(request, response);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

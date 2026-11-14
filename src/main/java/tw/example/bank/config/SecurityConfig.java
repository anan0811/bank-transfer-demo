package tw.example.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tw.example.bank.user.DemoUserRepository;

@Configuration
public class SecurityConfig {
    /**
     * Public pages get their own chain without HTTP Basic, so stale credentials cached by the
     * browser (for example an old password) cannot turn the home page or demo directory into a 401.
     */
    @Bean
    @Order(1)
    SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/", "/index.html", "/app.css", "/api/demo/accounts")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET).permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    // The web page sends "X-Requested-With: XMLHttpRequest"; for those requests Spring Security answers a
    // failed login with a plain 401 (no WWW-Authenticate header), so the browser never shows its own
    // login dialog or caches the credentials typed into it.
    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/me").authenticated()
                        // Separation of duties: customers move money, the administrator only reviews.
                        .requestMatchers(HttpMethod.GET, "/api/accounts", "/api/accounts/*/transfers").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/transfers").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(DemoUserRepository users) {
        return loginId -> users.findById(loginId)
                .map(user -> User.withUsername(user.getLoginId())
                        .password(user.getPasswordHash()).roles(user.getRole()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Demo account not found."));
    }
}

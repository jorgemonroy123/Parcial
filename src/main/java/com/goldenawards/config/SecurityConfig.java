package com.goldenawards.config;

import com.goldenawards.model.Usuario;
import com.goldenawards.repository.UsuarioRepository;
import com.goldenawards.security.jwt.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return username -> usuarioRepository.findByUsername(username)
                .filter(Usuario::isActivo)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRol())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado o inactivo"));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(auth -> auth
                // Público web + estáticos
                .requestMatchers("/", "/login", "/error", "/css/**", "/js/**").permitAll()
                .requestMatchers("/publico/**").permitAll()
                // API pública y gateway
                .requestMatchers("/api/estado", "/api/auth/**", "/api/publico/**", "/api/gateway/**").permitAll()
                // Roles
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/organizador/**", "/api/organizador/**").hasRole("ORGANIZADOR")
                .requestMatchers("/votante/**", "/api/votante/**").hasRole("VOTANTE")
                .requestMatchers("/nominado/**", "/api/nominado/**").hasRole("NOMINADO")
                .requestMatchers("/jurado/**", "/api/jurado/**").hasRole("JURADO")
                .requestMatchers("/auditor/**", "/api/auditor/**").hasRole("AUDITOR")
                .requestMatchers("/periodista/**", "/api/periodista/**").hasRole("PERIODISTA")
                .requestMatchers("/moderador/**", "/api/moderador/**").hasRole("MODERADOR")
                .requestMatchers("/representante/**", "/api/representante/**").hasRole("REPRESENTANTE")
                .requestMatchers("/soporte/**", "/api/soporte/**").hasRole("SOPORTE")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/panel", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            .httpBasic(Customizer.withDefaults())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

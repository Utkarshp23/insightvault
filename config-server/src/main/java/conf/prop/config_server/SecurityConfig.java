// package conf.prop.config_server;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.config.Customizer;

// @Configuration
// public class SecurityConfig {

//      @Bean
//     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//         http
//           .authorizeHttpRequests(auth -> auth
//             .requestMatchers("/actuator/health", "/actuator/info").permitAll()
//             .anyRequest().authenticated()
//           )
//           .httpBasic(Customizer.withDefaults())
//           .csrf().disable();
//         return http.build();
//     }
// }

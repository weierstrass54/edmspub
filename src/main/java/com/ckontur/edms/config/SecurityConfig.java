package com.ckontur.edms.config;

import com.auth0.jwt.algorithms.Algorithm;
import com.ckontur.edms.component.auth.JwtFilter;
import com.ckontur.edms.utils.RSAUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public Algorithm algorithm() {
        return RSAUtils.readPublicKeyFromResource("keys/public.pem")
            .flatMap(publicKey -> RSAUtils.readPrivateKeyFromResource("keys/private.pem").map(
                privateKey -> Algorithm.RSA512(publicKey, privateKey)
            ))
            .getOrElseThrow(t -> new RuntimeException("Не удалось прочитать ключи RSA-шифрования.", t));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .httpBasic().disable()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/", "/auth/**", "/v2/api-docs", "/swagger-ui/**", "/swagger-resources/**", "/actuator/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }
}

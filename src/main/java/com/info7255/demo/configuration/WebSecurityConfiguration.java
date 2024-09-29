package com.info7255.demo.configuration;

import com.info7255.demo.filter.AuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;


@SuppressWarnings("deprecation")
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthFilter authFilter;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().addFilterBefore(authFilter, FilterSecurityInterceptor.class)
                .authorizeRequests()
                .antMatchers( "/**").permitAll().
                anyRequest().authenticated().and().
                sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//				.and().
//		// make sure we use stateless session; session won't be used to
//		// store user's state.
//				exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and().sessionManagement()
//		.sessionCreationPolicy(SessionCreationPolicy.STATELESS);

//		http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    }

////
//    @Bean
//    @Override
//    public AuthenticationManager authenticationManagerBean() throws Exception  {
//        return super.authenticationManagerBean();
//    }

}

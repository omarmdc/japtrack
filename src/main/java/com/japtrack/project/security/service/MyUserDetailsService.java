package com.japtrack.project.security.service;

import com.japtrack.project.exception.custom.ResourceNotFoundException;
import com.japtrack.project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    public UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String userOrEmail) throws UsernameNotFoundException {

        if (userOrEmail.contains("@")) {
            return userRepository.findByUserEmail(userOrEmail)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ":( Sorry, we couldn't find user with email: " + userOrEmail));
        }
        else {
            return userRepository.findByUserName(userOrEmail)
                    .orElseThrow(()-> new ResourceNotFoundException(
                            ":( Sorry, we could't find user with username: " + userOrEmail));
        }
    }
}

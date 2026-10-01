package com.japtrack.project.security.service;

import com.japtrack.project.entity.User;
import com.japtrack.project.repository.UserRepository;
import com.japtrack.project.security.AuthenticatedUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public MyUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    // Users can log in with either their email or their username
    @Override
    public UserDetails loadUserByUsername(String userOrEmail) throws UsernameNotFoundException {

        Optional<User> user = userOrEmail.contains("@")
                ? userRepository.findByUserEmail(userOrEmail)
                : userRepository.findByUserName(userOrEmail);

        // Generic message on purpose: it must not reveal which accounts exist
        return user.map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}

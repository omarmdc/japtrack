package com.japtrack.project.security.service;

import com.japtrack.project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService {

    @Autowired
    public UserRepository userRepository;

    

}

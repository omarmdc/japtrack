package com.japtrack.project.repository;

import com.japtrack.project.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository <Application, Long> {

    // Search for the applications of a user based on the user's ID
    List<Application> findByUser_UserId(Long userId);

}

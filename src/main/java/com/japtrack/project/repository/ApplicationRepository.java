package com.japtrack.project.repository;

import com.japtrack.project.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository <Application, Long> {
}

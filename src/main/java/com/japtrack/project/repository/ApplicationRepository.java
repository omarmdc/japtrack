package com.japtrack.project.repository;

import com.japtrack.project.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Every query here is scoped to an owner. The service always passes the logged-in user's ID,
// never an ID from the request, so a user can only ever reach their own applications.
// (JpaRepository's inherited findById/delete are not owner-scoped, so the service doesn't use them for lookups.)
public interface ApplicationRepository extends JpaRepository <Application, Long> {

    // One application, but only if it belongs to this user. Another user's application
    // comes back empty, exactly like an ID that doesn't exist.
    Optional<Application> findByApplicationIdAndUser_UserId(Long applicationId, Long userId);

    // All of a user's applications, newest first: most recent dateApplied, then the most recently
    // created one (highest ID) when two share the same date
    List<Application> findByUser_UserIdOrderByDateAppliedDescApplicationIdDesc(Long userId);
}

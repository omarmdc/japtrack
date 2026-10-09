package com.japtrack.project.repository;

import com.japtrack.project.entity.Application;
import com.japtrack.project.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Runs only the JPA layer against an in-memory database; each test is rolled back afterwards
@DataJpaTest
class ApplicationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ApplicationRepository applicationRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void createUsers() {
        alice = persistUser("alice");
        bob = persistUser("bob");
    }

    private User persistUser(String userName) {
        User user = new User();
        user.setUserName(userName);
        user.setUserEmail(userName + "@example.com");
        user.setUserFirstName(userName);
        user.setUserLastName("Tester");
        user.setPasswordHash("{bcrypt}not-a-real-hash");
        return entityManager.persist(user);
    }

    private Application persistApplication(User owner, String companyName, LocalDate dateApplied) {
        Application application = new Application();
        application.setUser(owner);
        application.setCompanyName(companyName);
        application.setPositionTitle("Intern");
        application.setDateApplied(dateApplied);
        return entityManager.persist(application);
    }


    // findByApplicationIdAndUser_UserId

    @Test
    void findsAnApplicationForItsOwner() {
        Application application = persistApplication(alice, "Acme", LocalDate.of(2026, 9, 1));

        assertThat(applicationRepository.findByApplicationIdAndUser_UserId(application.getApplicationId(), alice.getUserId()))
                .get()
                .extracting(Application::getCompanyName)
                .isEqualTo("Acme");
    }

    @Test
    void doesNotFindAnApplicationForADifferentUser() {
        Application alicesApplication = persistApplication(alice, "Acme", LocalDate.of(2026, 9, 1));

        assertThat(applicationRepository.findByApplicationIdAndUser_UserId(alicesApplication.getApplicationId(), bob.getUserId()))
                .isEmpty();
    }

    @Test
    void doesNotFindAnApplicationThatDoesNotExist() {
        assertThat(applicationRepository.findByApplicationIdAndUser_UserId(999_999L, alice.getUserId()))
                .isEmpty();
    }


    // findByUser_UserIdOrderByDateAppliedDescApplicationIdDesc

    @Test
    void listsOnlyTheOwnersApplicationsNewestFirst() {
        Application oldest = persistApplication(alice, "Oldest", LocalDate.of(2026, 7, 1));
        Application newest = persistApplication(alice, "Newest", LocalDate.of(2026, 9, 15));
        Application middle = persistApplication(alice, "Middle", LocalDate.of(2026, 8, 10));
        persistApplication(bob, "Bob's", LocalDate.of(2026, 9, 30));

        List<Application> result = applicationRepository
                .findByUser_UserIdOrderByDateAppliedDescApplicationIdDesc(alice.getUserId());

        assertThat(result)
                .extracting(Application::getApplicationId)
                .containsExactly(newest.getApplicationId(), middle.getApplicationId(), oldest.getApplicationId());
    }

    @Test
    void sameDateApplicationsAreOrderedByMostRecentlyCreated() {
        LocalDate sameDay = LocalDate.of(2026, 9, 1);
        Application first = persistApplication(alice, "First", sameDay);
        Application second = persistApplication(alice, "Second", sameDay);

        assertThat(applicationRepository.findByUser_UserIdOrderByDateAppliedDescApplicationIdDesc(alice.getUserId()))
                .extracting(Application::getApplicationId)
                .containsExactly(second.getApplicationId(), first.getApplicationId());
    }

    @Test
    void listIsEmptyForAUserWithNoApplications() {
        persistApplication(alice, "Acme", LocalDate.of(2026, 9, 1));

        assertThat(applicationRepository.findByUser_UserIdOrderByDateAppliedDescApplicationIdDesc(bob.getUserId()))
                .isEmpty();
    }
}

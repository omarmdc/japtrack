package com.japtrack.project.controller;

import com.japtrack.project.entity.Application;
import com.japtrack.project.entity.User;
import com.japtrack.project.repository.ApplicationRepository;
import com.japtrack.project.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.japtrack.project.support.SpaCsrf.validToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// End-to-end ownership checks: two real users, real logins (session cookies) and real CSRF tokens.
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationOwnershipTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userA;
    private User userB;
    private MockHttpSession sessionA;
    private MockHttpSession sessionB;

    @BeforeEach
    void createUsersAndLogIn() throws Exception {
        userA = saveUser("alice");
        userB = saveUser("bob");
        sessionA = login("alice");
        sessionB = login("bob");
    }

    @AfterEach
    void cleanUp() {
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }


    // HELPERS

    private User saveUser(String userName) {
        User user = new User();
        user.setUserName(userName);
        user.setUserEmail(userName + "@example.com");
        user.setUserFirstName(userName);
        user.setUserLastName("Tester");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return userRepository.save(user);
    }

    private MockHttpSession login(String userName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .session(new MockHttpSession())
                        .param("username", userName)
                        .param("password", PASSWORD)
                        .with(validToken(mockMvc)))
                .andExpect(status().isNoContent())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private static String applicationJson(String companyName, String dateApplied) {
        return """
                {
                  "companyName": "%s",
                  "positionTitle": "Software Engineer Intern",
                  "status": "APPLIED",
                  "dateApplied": "%s",
                  "notes": "Original notes"
                }
                """.formatted(companyName, dateApplied);
    }

    // Creates an application through the API as the given user and returns its ID
    private Long createApplication(MockHttpSession session, String companyName, String dateApplied) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications")
                        .session(session)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson(companyName, dateApplied)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.applicationId")).longValue();
    }


    // Create

    @Test
    void createdApplicationBelongsToTheLoggedInUser() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .session(sessionA)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson("Acme", "2026-09-01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").isNumber())
                .andExpect(jsonPath("$.userId").value(userA.getUserId()))
                .andExpect(jsonPath("$.companyName").value("Acme"));

        assertThat(applicationRepository.findAll())
                .singleElement()
                .satisfies(application -> assertThat(application.getUser().getUserId()).isEqualTo(userA.getUserId()));
    }

    @Test
    void createRequestCannotChooseTheOwner() throws Exception {
        // Alice tries to create an application on Bob's account by adding Bob's userId to the body
        String json = """
                {
                  "userId": %d,
                  "companyName": "Sneaky Corp",
                  "positionTitle": "Intern",
                  "dateApplied": "2026-09-01"
                }
                """.formatted(userB.getUserId());

        mockMvc.perform(post("/api/applications")
                        .session(sessionA)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userA.getUserId()));

        // The extra field was ignored: the application is Alice's, and Bob has none
        assertThat(applicationRepository.findByUser_UserIdOrderByDateAppliedDescApplicationIdDesc(userA.getUserId())).hasSize(1);
        assertThat(applicationRepository.findByUser_UserIdOrderByDateAppliedDescApplicationIdDesc(userB.getUserId())).isEmpty();
    }

    @Test
    void invalidCreateInputReturns400() throws Exception {
        String json = """
                {
                  "companyName": "",
                  "jobPostUrl": "javascript:alert(1)",
                  "payRate": -5
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .session(sessionA)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.companyName").value("Company name is required"))
                .andExpect(jsonPath("$.fieldErrors.positionTitle").value("Position title is required"))
                .andExpect(jsonPath("$.fieldErrors.dateApplied").value("Date applied is required"))
                .andExpect(jsonPath("$.fieldErrors.jobPostUrl").value("Job post URL must be a valid http or https link"))
                .andExpect(jsonPath("$.fieldErrors.payRate").value("Pay rate cannot be negative"));

        assertThat(applicationRepository.count()).isZero();
    }


    // List

    @Test
    void listContainsOnlyTheLoggedInUsersApplicationsNewestFirst() throws Exception {
        Long older = createApplication(sessionA, "Alice Older", "2026-08-01");
        Long newer = createApplication(sessionA, "Alice Newer", "2026-09-15");
        createApplication(sessionB, "Bob Only", "2026-09-20");

        mockMvc.perform(get("/api/applications").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].applicationId").value(newer))
                .andExpect(jsonPath("$[1].applicationId").value(older))
                .andExpect(jsonPath("$[*].userId", everyItem(equalTo(userA.getUserId().intValue()))));
    }

    @Test
    void otherUsersApplicationsNeverAppearInTheList() throws Exception {
        createApplication(sessionB, "Bob Only", "2026-09-20");

        mockMvc.perform(get("/api/applications").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }


    // Read

    @Test
    void userCanReadTheirOwnApplication() throws Exception {
        Long applicationId = createApplication(sessionA, "Acme", "2026-09-01");

        mockMvc.perform(get("/api/applications/{id}", applicationId).session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId))
                .andExpect(jsonPath("$.companyName").value("Acme"));
    }

    @Test
    void readingAnotherUsersApplicationReturns404LikeAMissingOne() throws Exception {
        Long bobsApplication = createApplication(sessionB, "Bob Only", "2026-09-20");

        String othersResponse = mockMvc.perform(get("/api/applications/{id}", bobsApplication).session(sessionA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andReturn().getResponse().getContentAsString();

        // Same message shape as an ID that doesn't exist at all, so nothing reveals that Bob's exists
        String missingResponse = mockMvc.perform(get("/api/applications/{id}", 999_999).session(sessionA))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        assertThat(othersResponse).contains("couldn't find a job application with ID: " + bobsApplication);
        assertThat(missingResponse).contains("couldn't find a job application with ID: 999999");
    }


    // Update

    @Test
    void userCanUpdateTheirOwnApplication() throws Exception {
        Long applicationId = createApplication(sessionA, "Acme", "2026-09-01");

        mockMvc.perform(patch("/api/applications/{id}", applicationId)
                        .session(sessionA)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INTERVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"))
                // Fields that weren't sent stay the same
                .andExpect(jsonPath("$.companyName").value("Acme"))
                .andExpect(jsonPath("$.notes").value("Original notes"));
    }

    @Test
    void updatingAnotherUsersApplicationReturns404AndChangesNothing() throws Exception {
        Long bobsApplication = createApplication(sessionB, "Bob Only", "2026-09-20");

        mockMvc.perform(patch("/api/applications/{id}", bobsApplication)
                        .session(sessionA)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\": \"Hijacked\", \"notes\": \"Changed by Alice\"}"))
                .andExpect(status().isNotFound());

        Application unchanged = applicationRepository.findById(bobsApplication).orElseThrow();
        assertThat(unchanged.getCompanyName()).isEqualTo("Bob Only");
        assertThat(unchanged.getNotes()).isEqualTo("Original notes");
        assertThat(unchanged.getUser().getUserId()).isEqualTo(userB.getUserId());
    }

    @Test
    void invalidUpdateInputReturns400() throws Exception {
        Long applicationId = createApplication(sessionA, "Acme", "2026-09-01");

        mockMvc.perform(patch("/api/applications/{id}", applicationId)
                        .session(sessionA)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.companyName").value("Company name cannot be blank"));

        assertThat(applicationRepository.findById(applicationId).orElseThrow().getCompanyName()).isEqualTo("Acme");
    }


    // Delete

    @Test
    void deletingOwnApplicationReturns204() throws Exception {
        Long applicationId = createApplication(sessionA, "Acme", "2026-09-01");

        mockMvc.perform(delete("/api/applications/{id}", applicationId)
                        .session(sessionA)
                        .with(validToken(mockMvc)))
                .andExpect(status().isNoContent());

        assertThat(applicationRepository.existsById(applicationId)).isFalse();
    }

    @Test
    void deletingAnotherUsersApplicationReturns404AndKeepsIt() throws Exception {
        Long bobsApplication = createApplication(sessionB, "Bob Only", "2026-09-20");

        mockMvc.perform(delete("/api/applications/{id}", bobsApplication)
                        .session(sessionA)
                        .with(validToken(mockMvc)))
                .andExpect(status().isNotFound());

        assertThat(applicationRepository.existsById(bobsApplication)).isTrue();
        // And Bob still sees it
        mockMvc.perform(get("/api/applications/{id}", bobsApplication).session(sessionB))
                .andExpect(status().isOk());
    }


    // Removed / unauthenticated routes

    @Test
    void oldPerUserListRouteNoLongerExists() throws Exception {
        createApplication(sessionB, "Bob Only", "2026-09-20");

        // The route that took a userId in the path is gone, so it can't be used to read Bob's list
        mockMvc.perform(get("/api/applications/user/{userId}", userB.getUserId()).session(sessionA))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequestsReturn401() throws Exception {
        Long applicationId = createApplication(sessionA, "Acme", "2026-09-01");

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/applications/{id}", applicationId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/applications")
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson("Acme", "2026-09-01")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/applications/{id}", applicationId)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REJECTED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/applications/{id}", applicationId)
                        .with(validToken(mockMvc)))
                .andExpect(status().isUnauthorized());

        assertThat(applicationRepository.count()).isEqualTo(1);
        assertThat(applicationRepository.findById(applicationId).orElseThrow().getStatus().name()).isEqualTo("APPLIED");
    }
}

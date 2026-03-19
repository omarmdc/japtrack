package com.japtrack.project.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonPropertyOrder({"userId","userName","userEmail","userFirstName","userLastName","createdAt"})
@Data
@NoArgsConstructor
public class UserResponse {

    private Long userId;
    private String userName;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private LocalDate createdAt;
}
package com.example.diplom.model.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LoginResponse {

    @JsonProperty("auth-token")
    private final String authToken;
}

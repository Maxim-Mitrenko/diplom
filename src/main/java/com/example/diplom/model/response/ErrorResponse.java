package com.example.diplom.model.response;

import lombok.Data;

@Data
public class ErrorResponse {

    private final String message;
    private final int id;
}

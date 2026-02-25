package com.nazir.inventory.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;

    private T data;

    private String message;

    private Instant timestamp;
}

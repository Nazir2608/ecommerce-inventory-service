package com.nazir.inventory.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private Instant timestamp;

    private int status;

    private String error;

    private String path;
}

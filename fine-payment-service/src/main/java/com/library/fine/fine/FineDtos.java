package com.library.fine.fine;



import java.math.BigDecimal;

import java.time.Instant;

import java.time.LocalDate;



public class FineDtos {

    public record FineResponse(

            Long id,

            Long borrowId,

            Long userId,

            Long bookId,

            BigDecimal amount,

            String reason,

            String status,

            LocalDate lastCalculatedDate,

            Instant updatedAt

    ) {}

}


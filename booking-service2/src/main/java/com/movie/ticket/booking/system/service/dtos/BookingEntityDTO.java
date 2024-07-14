package com.movie.ticket.booking.system.service.dtos;

import com.movie.ticket.booking.system.service.enums.BookingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;


@Data
@Validated
@Builder
public class BookingEntityDTO {

    private UUID bookingId;
    @NotBlank(message = "please provide user Id")
    private String userId;
    @NotNull(message = "please provide movie Id")
    @Positive(message = "please provide vlaid movie Id")
    private Integer movieId;
    @NotNull(message = "You need to select atleast one seat to create a booking")
    private List<String> seatsSelected;
    @NotNull(message = "Select the show date")
    private LocalDate showDate;
    @NotNull(message = "Select the show Time")
    private LocalTime showTime;
    private BookingStatus bookingStatus;
    @NotNull(message = "booking amount id is mandatory")
    @Positive(message = "Booking amount must be a positive value")
    private Double bookingAmount;

}

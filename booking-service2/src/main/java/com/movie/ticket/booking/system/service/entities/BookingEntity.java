package com.movie.ticket.booking.system.service.entities;

import com.movie.ticket.booking.system.service.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class BookingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID bookingId;
    private String userId;
    private Integer movieId;
    @ElementCollection
    private List<String> seatsSelected;
    private LocalDate showDate;
    private LocalTime showTime;
    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;
    private Double bookingAmount;



}

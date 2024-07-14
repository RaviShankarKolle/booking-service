package com.movie.ticket.booking.system.service.apis;

import com.movie.ticket.booking.system.service.dtos.BookingEntityDTO;
import com.movie.ticket.booking.system.service.dtos.ResponseDTO;
import com.movie.ticket.booking.system.service.entities.BookingEntity;
import com.movie.ticket.booking.system.service.services.BookingUserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("booking")
@Slf4j

public class BookingAPI {
    @Autowired
    private BookingUserService service;

    @PostMapping
    public ResponseEntity<ResponseDTO> createBooking(@Valid  @RequestBody BookingEntityDTO bookingDTO){
        log.info("Enter into booking api");
        ResponseDTO responseDTO=service.createBooking(bookingDTO);
        return new ResponseEntity<ResponseDTO>(responseDTO, HttpStatus.CREATED);


    }

}

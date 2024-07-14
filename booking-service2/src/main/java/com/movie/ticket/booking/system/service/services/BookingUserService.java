package com.movie.ticket.booking.system.service.services;

import com.movie.ticket.booking.system.service.dtos.BookingEntityDTO;
import com.movie.ticket.booking.system.service.dtos.ResponseDTO;

public interface BookingUserService {
    public ResponseDTO createBooking(BookingEntityDTO bookingDTO);
}


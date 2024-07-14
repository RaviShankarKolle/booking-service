package com.movie.ticket.booking.system.service.services;

import com.movie.ticket.booking.system.service.brokers.PaymentServiceBroker;
import com.movie.ticket.booking.system.service.dtos.BookingEntityDTO;
import com.movie.ticket.booking.system.service.dtos.ResponseDTO;
import com.movie.ticket.booking.system.service.entities.BookingEntity;
import com.movie.ticket.booking.system.service.enums.BookingStatus;
import com.movie.ticket.booking.system.service.repositories.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.print.Book;

import static com.movie.ticket.booking.system.service.enums.BookingStatus.PENDING;

@Service
@Slf4j
public class BookingUserServiceImpl implements BookingUserService {

    @Autowired
    private BookingRepository repo;

    @Autowired
    private PaymentServiceBroker paymentService;

    @Override
    public ResponseDTO createBooking(@NotNull BookingEntityDTO bookingDTO) {
        log.info("Enter into the Service implements class");
        /*
        BookingEntity bentity=new BookingEntity();
        bentity.setBookingStatus(BookingStatus.PENDING);

        BeanUtils.copyProperties(bookingDTO,bentity);
        repo.save(bentity);
        */
       BookingEntity bookingEntity= BookingEntity.builder()
                 .movieId(bookingDTO.getMovieId())
               .userId(bookingDTO.getUserId())
                 .bookingAmount(bookingDTO.getBookingAmount())
                 .bookingStatus(PENDING)
                 .showDate(bookingDTO.getShowDate())
                 .showTime(bookingDTO.getShowTime())
                 .seatsSelected(bookingDTO.getSeatsSelected())
                 .build();
       repo.save(bookingEntity);

       String paymentResponse=paymentService.createPayments();

       return ResponseDTO.builder()
               .bookingDTO(BookingEntityDTO.builder()
                       .userId(bookingEntity.getUserId())
                       .movieId(bookingEntity.getMovieId())
                       .bookingId(bookingEntity.getBookingId())
                       .bookingAmount(bookingEntity.getBookingAmount())
                       .bookingStatus(bookingEntity.getBookingStatus())
                       .showDate(bookingEntity.getShowDate())
                       .showTime(bookingEntity.getShowTime())
                       .seatsSelected(bookingEntity.getSeatsSelected())
                       .build())
               .build();

    }
}

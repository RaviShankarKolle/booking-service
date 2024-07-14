package com.movie.ticket.booking.system.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BookingService2Application {

	public static void main(String[] args) {

		SpringApplication.run(BookingService2Application.class, args);
	}

}

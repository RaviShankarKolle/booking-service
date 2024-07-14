package com.movie.ticket.booking.system.service.handlers;

import com.movie.ticket.booking.system.service.dtos.ResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.web.bind.MethodArgumentNotValidException.*;

@RestControllerAdvice
@Slf4j
public class  BookingAPIHandler {

    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ResponseDTO> methodArgumentNotValidException(MethodArgumentNotValidException exception) {
        log.info("Enter into BookingAPIHandler" + exception.getMessage());
//
//   List<ObjectError> errors= exception.getBindingResult().getAllErrors();
//       List<String> errorMessages=new ArrayList<>();
//
//       for(ObjectError error:errors){
//           errorMessages.add(error.getDefaultMessage());
//       }
//
//
//        return new ResponseEntity<ResponseDTO>(ResponseDTO.builder()
//                .errorMessages(errorMessages
//                )
//                .build()
//                , HttpStatus.BAD_REQUEST);


        return new ResponseEntity<ResponseDTO>(ResponseDTO.builder()
                .errorDescription(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .errorMessages(
                        exception.getBindingResult().getAllErrors()
                                .stream()
                                .map(ObjectError::getDefaultMessage)
                                .collect(Collectors.toList())
                )
                .build()
                , HttpStatus.BAD_REQUEST);

    }

}



package com.jq.wa2pdf.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.repository.Repository;

@ControllerAdvice
public class GlobalExceptionHandler {
	@Autowired
	private Repository repository;

	@ExceptionHandler(Throwable.class)
	public ResponseEntity<Object> handleAllExceptions(Throwable ex, WebRequest request) {
		final Ticket ticket = new Ticket();
		ticket.setNote(request.getDescription(false) + "\n" + Utilities.stackTraceToString(ex));
		repository.save(ticket);
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}
}

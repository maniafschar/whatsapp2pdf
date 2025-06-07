package com.jq.wa2pdf.service;

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

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
		// Log exception with context information
		final Ticket ticket = new Ticket();
		ticket.setNote("Exception occurred: " + ex.getMessage() + "\nRequest URI: " + request.getDescription(false));
		repository.save(ticket);
		return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
	}
}

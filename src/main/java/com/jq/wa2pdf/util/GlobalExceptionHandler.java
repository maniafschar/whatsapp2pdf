package com.jq.wa2pdf.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.service.AdminService;

@ControllerAdvice
public class GlobalExceptionHandler {
	@Autowired
	private AdminService adminService;

	@ExceptionHandler(Throwable.class)
	public ResponseEntity<Object> handleAllExceptions(final Throwable ex, final WebRequest request) {
		this.adminService.createTicket(new Ticket(request.getDescription(false) + "\n" + Utilities.stackTraceToString(ex)));
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}
}

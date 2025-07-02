package com.jq.wa2pdf.api;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jq.wa2pdf.entity.Log;
import com.jq.wa2pdf.service.AdminService;
import com.jq.wa2pdf.service.AdminService.AdminData;

@RestController
@RequestMapping("sc")
public class AdminApi {
	@Autowired
	private AdminService adminService;

	@GetMapping("init")
	public AdminData init() {
		return this.adminService.init();
	}

	@GetMapping("log")
	public List<Log> log(@RequestParam final String search) {
		return this.adminService.log(search);
	}

	@PostMapping("build/{type}")
	public String build(@PathVariable final String type) throws IOException {
		return this.adminService.build(type);
	}

	@DeleteMapping("ticket/{id}")
	public void deleteTicket(@PathVariable final BigInteger id) {
		this.adminService.deleteTicket(id);
	}
}

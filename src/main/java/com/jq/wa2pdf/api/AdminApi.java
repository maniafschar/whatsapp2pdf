package com.jq.wa2pdf.api;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	@PostMapping("build")
	public String build(@PathVariable final String type) throws IOException {
		return this.adminService.build(type);
	}
}
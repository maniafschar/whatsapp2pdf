package com.jq.wa2pdf.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jq.wa2pdf.service.AdminService;
import com.jq.wa2pdf.service.AdminService.AdminData;

@RestController
@RequestMapping("sc")
public class ApplicationApi {
	@Autowired
	private AdminService adminService;

	@GetMapping("init")
	public AdminData init() throws Exception {
		return this.adminService.init();
	}
}

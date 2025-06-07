package com.jq.wa2pdf.entity;

import jakarta.persistence.Entity;

@Entity
public class Ticket extends BaseEntity {
	private String note;

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}
}
package com.jq.wa2pdf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Ticket extends BaseEntity {
	@Column(columnDefinition = "TEXT")
	private String note;

	public Ticket() {
		super();
	}

	public Ticket(final String note) {
		this();
		setNote(note);
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note.length() > 1000 ? note.substring(0, 1000) : note;
	}
}

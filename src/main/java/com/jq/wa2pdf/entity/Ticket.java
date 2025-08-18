package com.jq.wa2pdf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Ticket extends BaseEntity {
	@Column(columnDefinition = "TEXT")
	private String note;
	private boolean deleted = false;

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
		this.note = note.length() > 2000 ? note.substring(0, 1999) + "…" : note;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}
}

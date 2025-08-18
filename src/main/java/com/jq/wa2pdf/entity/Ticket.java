package com.jq.wa2pdf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import com.jq.wa2pdf.util.Utilities;

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
		this.note = Utilities.trim(note, 2000);
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}
}

package com.jq.wa2pdf.entity;

import com.jq.wa2pdf.util.Utilities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Ticket extends BaseEntity {
	public static final String ERROR = "ERROR\n";
	@Column(columnDefinition = "TEXT")
	private String note;
	private boolean deleted = false;

	public Ticket() {
		super();
	}

	public Ticket(final String note) {
		this();
		this.setNote(note);
	}

	public String getNote() {
		return this.note;
	}

	public void setNote(final String note) {
		this.note = Utilities.trim(note, 2000);
	}

	public boolean isDeleted() {
		return this.deleted;
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}
}

package com.jq.wa2pdf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Feedback extends BaseEntity {
	@Column(columnDefinition = "TEXT")
	private String note;
	@Column(columnDefinition = "TEXT")
	private String answer;
	private String name;
	private String email;
	private String image;
	private String pin;
	private short rating;
	private boolean verified = false;

	public String getNote() {
		return this.note;
	}

	public void setNote(final String note) {
		this.note = note.length() > 1000 ? note.substring(0, 1000) : note;
	}

	public String getAnswer() {
		return this.answer;
	}

	public void setAnswer(final String answer) {
		this.answer = answer.length() > 1000 ? answer.substring(0, 1000) : answer;
	}

	public String getImage() {
		return this.image;
	}

	public void setImage(final String image) {
		this.image = image;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public String getPin() {
		return this.pin;
	}

	public void setPin(final String pin) {
		this.pin = pin;
	}

	public short getRating() {
		return this.rating;
	}

	public void setRating(final short rating) {
		this.rating = rating;
	}

	public boolean isVerified() {
		return this.verified;
	}

	public void setVerified(final boolean verified) {
		this.verified = verified;
	}
}
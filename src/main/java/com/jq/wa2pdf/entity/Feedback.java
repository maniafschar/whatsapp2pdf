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

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note.length() > 1000 ? note.substring(0, 1000) : note;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(final String answer) {
		this.answer = answer.length() > 1000 ? answer.substring(0, 1000) : answer;
	}

	public String getImage() {
		return image;
	}

	public void setImage(final String image) {
		this.image = image;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public String getPin() {
		return pin;
	}

	public void setPin(final String pin) {
		this.pin = pin;
	}

	public short getRating() {
		return rating;
	}

	public void setRating(final short rating) {
		this.rating = rating;
	}
}

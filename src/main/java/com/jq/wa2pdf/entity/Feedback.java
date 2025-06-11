package com.jq.wa2pdf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Feedback extends BaseEntity {
	@Column(columnDefinition = "TEXT")
	private String note;
	@Column(columnDefinition = "TEXT")
	private String answer;
	private short rating;
	private short image;
	private String pin;

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

	public String getPin() {
		return pin;
	}

	public void setPin(final String pin) {
		this.pin = pin;
	}

	public short getRating() {
		return rating;
	}

	public void setNote(final short rating) {
		this.rating = rating;
	}
}

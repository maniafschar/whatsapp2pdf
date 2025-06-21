package com.jq.wa2pdf.entity;

import java.math.BigInteger;
import java.sql.Timestamp;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private BigInteger id;

	private Timestamp createdAt;
	private Timestamp modifiedAt;

	public BigInteger getId() {
		return this.id;
	}

	public void setId(final BigInteger id) {
		this.id = id;
	}

	public Timestamp getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(final Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Timestamp getModifiedAt() {
		return this.modifiedAt;
	}

	public void setModifiedAt(final Timestamp modifiedAt) {
		this.modifiedAt = modifiedAt;
	}
}
package com.jq.wa2pdf.repository;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jq.wa2pdf.entity.BaseEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

@org.springframework.stereotype.Repository
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Component
public class Repository {
	@PersistenceContext
	private EntityManager em;

	public <T> List<T> list(final String hql, final Class<T> clazz) {
		return this.em.createQuery(hql, clazz).getResultList();
	}

	public <T extends BaseEntity> T one(final Class<T> clazz, final BigInteger id) {
		return this.em.find(clazz, id);
	}

	public void save(final BaseEntity entity) throws IllegalArgumentException {
		try {
			if (entity.getId() == null) {
				if (entity.getCreatedAt() == null)
					entity.setCreatedAt(new Timestamp(Instant.now().toEpochMilli()));
				this.em.persist(entity);
			} else {
				entity.setModifiedAt(new Timestamp(Instant.now().toEpochMilli()));
				this.em.merge(entity);
			}
			this.em.flush();
		} catch (final PersistenceException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void delete(final BaseEntity entity) throws IllegalArgumentException {
		try {
			this.em.remove(this.em.contains(entity) ? entity : this.em.merge(entity));
		} catch (final PersistenceException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void executeUpdate(final String hql, final Object... params) {
		final jakarta.persistence.Query query = this.em.createQuery(hql);
		if (params != null) {
			for (int i = 0; i < params.length; i++)
				query.setParameter(i + 1, params[i]);
		}
		query.executeUpdate();
	}
}

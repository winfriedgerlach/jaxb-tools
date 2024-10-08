package org.jvnet.hyperjaxb3.ejb.tutorials.po.stepone;

import generated.ObjectFactory;
import generated.PurchaseOrderType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JPATest {

	private ObjectFactory objectFactory;

	private EntityManagerFactory entityManagerFactory;

    @BeforeEach
	public void setUp() throws Exception {

		objectFactory = new ObjectFactory();

		final Properties persistenceProperties = new Properties();
		InputStream is = null;
		try {
			is = getClass().getClassLoader().getResourceAsStream(
					"persistence.properties");
			persistenceProperties.load(is);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ignored) {

				}
			}
		}

		entityManagerFactory = Persistence.createEntityManagerFactory(
				"generated", persistenceProperties);
	}

    @Test
	public void testSaveAndLoad() {
		final PurchaseOrderType alpha = objectFactory.createPurchaseOrderType();
		alpha.setShipTo(objectFactory.createUSAddress());
		alpha.getShipTo().setCity("Sacramento");

		final EntityManager saveManager = entityManagerFactory
				.createEntityManager();
		saveManager.getTransaction().begin();
		saveManager.persist(alpha);
		saveManager.getTransaction().commit();
		saveManager.close();

		final Long id = alpha.getHjid();

		final EntityManager loadManager = entityManagerFactory
				.createEntityManager();
		final PurchaseOrderType beta = loadManager.find(
				PurchaseOrderType.class, id);
		loadManager.close();
		// Check that we're still shipping to Sacramento
		Assertions.assertEquals("Sacramento", beta.getShipTo().getCity());

	}

}

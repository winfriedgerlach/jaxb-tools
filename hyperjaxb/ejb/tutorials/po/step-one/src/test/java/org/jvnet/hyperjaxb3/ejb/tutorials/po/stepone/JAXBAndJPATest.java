package org.jvnet.hyperjaxb3.ejb.tutorials.po.stepone;

import generated.ObjectFactory;
import generated.PurchaseOrderType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.jaxb.lang.ExtendedJAXBEqualsStrategy;

public class JAXBAndJPATest {

	private ObjectFactory objectFactory;

	private EntityManagerFactory entityManagerFactory;

	private JAXBContext context;

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

		context = JAXBContext.newInstance("generated");
	}

	@Test
	public void testRoundtrip() throws JAXBException {

		final Unmarshaller unmarshaller = context.createUnmarshaller();
		final Object object = unmarshaller.unmarshal(new File(
				"src/test/samples/po.xml"));
		final PurchaseOrderType alpha = ((JAXBElement<PurchaseOrderType>) object)
				.getValue();

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
		// Using not default equals strategy since BigDecimal has errors in equals strict equality
		Assertions.assertTrue(
            alpha.equals(null, null, beta, ExtendedJAXBEqualsStrategy.INSTANCE),
            "Objects are not equal.");

		final Marshaller marshaller = context.createMarshaller();
		marshaller.marshal(objectFactory.createPurchaseOrder(beta), System.out);
		loadManager.close();
	}
}

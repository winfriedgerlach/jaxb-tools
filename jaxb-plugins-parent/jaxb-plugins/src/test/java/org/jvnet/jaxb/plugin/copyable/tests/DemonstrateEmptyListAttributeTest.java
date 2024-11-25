package org.jvnet.jaxb.plugin.copyable.tests;

import com.sun.tools.xjc.Options;
import org.apache.maven.plugin.Mojo;
import org.junit.jupiter.api.Test;
import org.jvnet.jaxb.maven.AbstractXJCMojo;
import org.jvnet.jaxb.maven.test.RunXJCMojo;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemonstrateEmptyListAttributeTest extends RunXJCMojo {

	@Override
	protected void configureMojo(AbstractXJCMojo<Options> mojo) {
		super.configureMojo(mojo);
		mojo.setForceRegenerate(true);
	}

	@Override
	public List<String> getArgs() {
		final List<String> args = new ArrayList<String>(super.getArgs());
		args.add("-Xcopyable");
		return args;
	}

    @Override
    public File getSchemaDirectory() {
        return new File(getBaseDir(), "src/test/resources/emptyAttributeList");
    }

    @Test
    void testEmptyListAttribute() throws Exception {
        // WHEN the plugin is executed
        final Mojo mojo = initMojo();
        mojo.execute();

        // THEN FooElement.java is generated
        var fooElementJavaFile = new File(getGeneratedDirectory(), "org/jvnet/jaxb/plugin/copyable/tests/FooElement.java");
        assertTrue(fooElementJavaFile.exists());

        // WHEN FooElement.java is compiled
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, fooElementJavaFile.getPath());

        // THEN FooElement.class exists
        var fooElementClassFile = new File(getGeneratedDirectory(), "org/jvnet/jaxb/plugin/copyable/tests/FooElement.class");
        assertTrue(fooElementClassFile.exists());

        // WHEN the parent folder of FooElement class is added to the classpath
        URLClassLoader.newInstance(new URL[] { fooElementClassFile.getParentFile().toURI().toURL() });

        // THEN the FooElement class can be loaded
        Class<?> fooElementClass = Class.forName("org.jvnet.jaxb.plugin.copyable.tests.FooElement");
        assertNotNull(fooElementClass);

        // ------------ OK, the real test starts now

        // GIVEN a FooElement with an empty BarAttribute (this is the problematic one)
        var elementWithEmptyAttributeXml = "<FooElement BarAttribute=\"\"/>";
        // AND a FooElement with some list elements in the BarAttribute (normal case, should work fine)
        var elementWithValuesInAttributeXml = "<FooElement BarAttribute=\"a b c\"/>";
        // AND an invalid FooElement without BarAttribute
        var elementWithoutListAttributeXml = "<FooElement />";

        // AND a validating unmarshaller for the class
        JAXBContext context = JAXBContext.newInstance(fooElementClass);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new StreamSource(new File(getSchemaDirectory(), "emptyAttributeList.xsd")));
        unmarshaller.setSchema(schema);

        // EXPECT the invalid FooElement cannot be unmarshalled, showing that XML validation is working
        assertThrows(UnmarshalException.class,
            () -> unmarshaller.unmarshal(new StringReader(elementWithoutListAttributeXml)));

        // WHEN the element with values in attribute is unmarshalled
        var elementWithValuesInAttribute = unmarshaller.unmarshal(new StringReader(elementWithValuesInAttributeXml));

        // THEN the list from the attribute is filled as expected
        // (use field directly instead of getter, as the generated getter would convert null values to empty lists)
        var barAttributeField = fooElementClass.getDeclaredField("barAttribute");
        assertEquals(List.of("a", "b", "c"), barAttributeField.get(elementWithValuesInAttribute));

        // WHEN the element with empty attribute is unmarshalled
        var elementWithEmptyAttribute = unmarshaller.unmarshal(new StringReader(elementWithEmptyAttributeXml));

        // THEN the list from the attribute is empty
        assertEquals(List.of(), barAttributeField.get(elementWithEmptyAttribute));

        // WHEN both FooElements (the one with values and the one with empty list in attribute) are copied
        var cloneMethod = fooElementClass.getMethod("clone"); // clone() invokes copyTo(createNewInstance())
        var clonedElementWithEmptyAttribute = cloneMethod.invoke(elementWithEmptyAttribute);
        var clonedElementWithValuesInAttribute = cloneMethod.invoke(elementWithValuesInAttribute);

        // THEN the list from the attribute with values is copied as expected
        assertEquals(List.of("a", "b", "c"), barAttributeField.get(clonedElementWithValuesInAttribute));
        // AND the copied list from the empty attribute is not empty but null
        // (!!! we don't like that, but continue with test to show resulting problem)
        assertNull(barAttributeField.get(clonedElementWithEmptyAttribute));

        // WHEN the copied objects are serialized again with a validating marshaller
        var marshaller = context.createMarshaller();
        marshaller.setSchema(schema);
        // THEN the copied object with values in the attribute is serialized just fine
        var sw1 = new StringWriter();
        marshaller.marshal(clonedElementWithValuesInAttribute, sw1);
        System.out.println(sw1);
        // AND the copied object with originally empty (but now null) attribute throws an exception because it fails XML validation
        marshaller.marshal(clonedElementWithEmptyAttribute, new StringWriter());

        // long story short: because the generated copyTo method copies empty lists to null, valid XML becomes invalid
        // after round-trip (unmarshal -> copy -> marshal)
    }

}

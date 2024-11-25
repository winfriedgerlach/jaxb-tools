package org.jvnet.jaxb.xjc.model.concrete.tests.alpha;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "KnownReferencedType", propOrder = { "str" })
public class KnownReferencedType implements Serializable {
	protected String str;

	public String getStr() {
		return str;
	}

	public void setStr(String value) {
		this.str = value;
	}

}

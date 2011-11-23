package com.bookofbrilliantthings.spidron.sandbox;

import java.io.FileInputStream;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class TreeBuilder
{
	private HashMap<String, Class<?>> rootMap;
	private HashMap<String, Object> objectMap;
	
	public TreeBuilder(HashMap<String, Class<?>> rootMap)
	{
		this.rootMap = rootMap;
		objectMap = new HashMap<String, Object>();
	}

	private Document loadXml(String filename, String errorMsg)
	{
		/* set up to read an XML file */
		Document document = null;
		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
			FileInputStream fileInputStream = new FileInputStream(filename);
			InputSource inputSource = new InputSource(fileInputStream);
			document = documentBuilder.parse(inputSource);
		}
		catch(Exception e)
		{
			throw new Error(errorMsg + " " + filename, e);
		}
		
		return document;
	}
	
	public void loadObjects(String filename)
	{
		Document document = loadXml(filename, "error loading objects file");
		Element documentElement = document.getDocumentElement();
		if (!documentElement.getTagName().equals("Modules")) // TODO genericize
			throw new Error("Not a Modules document");
		
		for(Node node = documentElement.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue; /* TODO better error detection */
			
			Element element = (Element)node;
			String id = element.getAttribute("id");
			if (id == null)
				throw new Error(element.getTagName() + " has no id attribute");
			Object object = buildObject(element);
			Object other = objectMap.put(id, object);
			if (other != null)
				throw new Error("Object with id " + id + " multiply defined");
		}
	}
	
	private Object buildObject(Element element)
	{
		String baseTypeName = element.getTagName();
		
		Class<?> classObject = rootMap.get(baseTypeName);
		Class<Configurable> configurableClass = Configurable.class;
		if (!configurableClass.isAssignableFrom(classObject))
			throw new Error("object is not derived from Configurable");

		Object object = null;
		try
		{
			/* instantiate */
			object = classObject.newInstance();
			if (object == null)
				throw new Error("unable to instantiate object of class " + baseTypeName);

			Configurable configurable = Configurable.class.cast(object);
			configurable.configure(element);
		}
		catch(Exception e)
		{
			throw new Error("unable to build object " + baseTypeName + " of class " +
						classObject.getName(), e);
		}
		
		return object;
	}
	
	public <T> T getTree(String id, Class<T> klass)
	{
		Object tree = objectMap.get(id);
		if (tree == null)
			return null;
		if (!klass.isInstance(tree))
			return null;
		return klass.cast(tree);
	}
}

package com.bookofbrilliantthings.spidron.sandbox;

import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
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

		Object object = null;
		try
		{
			/* instantiate */
			object = classObject.newInstance();
			if (object == null)
				throw new Error("unable to instantiate object of class " + baseTypeName);

			setParameters(object, element);
		}
		catch(Exception e)
		{
			throw new Error("unable to build object " + baseTypeName + " of class " +
						classObject.getName(), e);
		}
		
		return object;
	}
	
	private void setParameters(Object object, Element objectElement)
	{
		Class<?> objClass = object.getClass();
		Method[] objMethods = objClass.getMethods();
		String objClassName = objClass.getName();
		String objElementName = objectElement.getTagName();
		
		try
		{
			/* keep track of the parameters we've found */
			HashMap<String, Object> foundParams = null;
			
			/*
			 * Run through the supplied configuration, and apply it.  Before applying, check it
			 * against the specified defaults.
			 */
			boolean hadText = false;
			for(Node paramNode = objectElement.getFirstChild(); paramNode != null;
				paramNode = paramNode.getNextSibling())
			{
				int nodeType = paramNode.getNodeType();
				if (nodeType == Node.TEXT_NODE)
				{
					Text textNode = (Text)paramNode;
					String text = textNode.getWholeText();
					text = text.trim();
					if (text.length() == 0)
						continue; // just whitespace, probably newlines
			
					/*
					 * By convention, methods that take a String must have a setString method.
					 */
					Method setString = objClass.getMethod("setString", String.class);
					if (setString == null)
						throw new Error("no setString() method available on " + objClass.getName());
					setString.invoke(object, text);
					hadText = true;
					
					/*
					 * For literals, we have to take the literal class, and set that for
					 * the current parameter; set object as the parameter.
					 */
					// TODO
					
					continue;
				}
				
				if (paramNode.getNodeType() != Node.ELEMENT_NODE)
					continue; // TODO more error detections
				
				if (hadText)
					throw new Error("can't have both text and elements");
				
				Element param = (Element)paramNode;
				String paramTag = param.getTagName();

				/* find the setter */
				String setMethodName = "set" + paramTag;
				Method setterMethod = null;
				SpidronParameter spidronParameter = null;
				for(Method method:objMethods)
				{
					String methodName = method.getName();
					if (methodName.equals(setMethodName))
					{
						setterMethod = method;
						spidronParameter = method.getAnnotation(SpidronParameter.class);
						break;
					}
				}
				if (setterMethod == null)
					throw new Error("no such parameter " + paramTag + " (no setter found)");

				/* figure out what class to use */
				String paramClassName = param.getAttribute("class");
				if ((paramClassName == null) || (paramClassName.length() == 0))
				{
					/* check for a default */
					if (spidronParameter != null)
						paramClassName = spidronParameter.defaultClass();
					if (paramClassName == null)
						throw new Error("no class specified for parameter " + paramTag +
								" and no default class ");
				}
				
				Class<?> paramClass = Class.forName(paramClassName);
				if (paramClass == null)
					throw new Error("Can't find parameter class " + paramClassName);
				
				Object paramObject = paramClass.newInstance();
				setParameters(paramObject, param);
				
				if (foundParams == null)
					foundParams = new HashMap<String, Object>();
				Object otherParam = foundParams.put(paramTag, paramObject);
				if (otherParam != null)
					throw new Error("object parameter " + param.getTagName() + " appears more than once");
				
				/* set the parameter */
				setterMethod.invoke(object, paramObject);
			}
			
			/* check to make sure we specified all the required parameters */
			for(Method method:objMethods)
			{
				SpidronParameter spidronParameter = method.getAnnotation(SpidronParameter.class);
				if ((spidronParameter != null) && spidronParameter.required())
				{
					String methodName = method.getName();
					if (!methodName.startsWith("set"))
						throw new Error("@SpidronParameter is not a setter");
					String paramName = methodName.substring(3); // skip over "set"
					Object found = foundParams.get(paramName);
					if (found == null)
						throw new Error("missing required parameter " + paramName);
				}
			}
		}
		catch(Exception e)
		{
			throw new Error("couldn't set parameters for " + objectElement.getTagName(), e);
		}
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

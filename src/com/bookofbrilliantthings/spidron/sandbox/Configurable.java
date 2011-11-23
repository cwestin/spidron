package com.bookofbrilliantthings.spidron.sandbox;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class Configurable
{
	private int line;
	
	public void setLine(int line)
	{
		this.line = line;
	}
	
	public int getLine()
	{
		return line;
	}
	
	public void configure(Element objectElement)
	{
		Class<?> objClass = getClass();
		Method[] objMethods = objClass.getMethods();
		String objClassName = objClass.getName();
		String objElementName = objectElement.getTagName();
		
		try
		{
			/* keep track of the parameters we've found */
			HashMap<String, Configurable> foundParams = null;
			
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
					setString.invoke(this, text);
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
				Class<Configurable> configurableClass = Configurable.class;
				if (!configurableClass.isAssignableFrom(paramClass))
					throw new Error("parameter class is not derived from Configurable");
				
				Object paramObject = paramClass.newInstance();
				Configurable configurableParam = configurableClass.cast(paramObject); 
				configurableParam.configure(param);
				
				if (foundParams == null)
					foundParams = new HashMap<String, Configurable>();
				Object otherParam = foundParams.put(paramTag, configurableParam);
				if (otherParam != null)
					throw new Error("object parameter " + param.getTagName() + " appears more than once");
				
				/* set the parameter */
				setterMethod.invoke(this, configurableParam);
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
}

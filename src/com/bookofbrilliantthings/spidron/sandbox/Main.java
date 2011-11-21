package com.bookofbrilliantthings.spidron.sandbox;

import java.util.HashMap;


/*
 * Just a simple driver for now
 */
public class Main
{
	public static void main(String[] arg)
	{
		//System.out.println("cwd: " + System.getProperty("user.dir"));
		
		HashMap<String, Class<?>> rootMap = new HashMap<String, Class<?>>();
		rootMap.put("Module", com.bookofbrilliantthings.spidron.sandbox.ModuleBasic.class);
		
		TreeBuilder treeBuilder = new TreeBuilder(rootMap);
		
		// TODO this should take streams, DOM documents, etc, so it can be done dynamically for my.y.c case
		treeBuilder.loadObjects("src/com/bookofbrilliantthings/spidron/sandbox/SomeModules.xml");
		
		Module module = treeBuilder.getTree("rss", Module.class);
		RequestContext reqCtx = new RequestContext();
		module.run(reqCtx);
	}
}

package com.bookofbrilliantthings.spidron.sandbox;

public class ModuleBasic
	extends Configurable
	implements Module
{
	private Source source;
	
	@SpidronParameter(
			defaultClass="com.bookofbrilliantthings.spidron.sandbox.SourceWebService",
			required=true)
	public void setSource(Source source)
	{
		this.source = source;
	}
	
	@Override
	public void run(RequestContext reqCtx)
	{
		// TODO Auto-generated method stub
		source.run(reqCtx);
	}
}

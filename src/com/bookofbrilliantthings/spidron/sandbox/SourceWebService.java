package com.bookofbrilliantthings.spidron.sandbox;

public class SourceWebService
	extends Configurable
	implements Source
{
	private StringValue urlValue;
	
	@SpidronParameter(
			defaultClass="com.bookofbrilliantthings.spidron.sandbox.StringLiteral",
			required=true)
	public void setUrl(StringValue stringValue)
	{
		urlValue = stringValue;
	}
	
	@Override
	public void run(RequestContext reqCtx)
	{
		// TODO Auto-generated method stub
		System.out.println("simulated fetch of " + urlValue.get(reqCtx));
	}
}

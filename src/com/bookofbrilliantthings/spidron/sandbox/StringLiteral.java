package com.bookofbrilliantthings.spidron.sandbox;

public class StringLiteral
	extends Configurable
	implements StringValue
{
	private String value;

	public StringLiteral()
	{
	}
	
	@Override
	public String get(RequestContext reqCtx)
	{
		return value;
	}
	
	public void setString(String value)
	{
		this.value = value;
	}
}

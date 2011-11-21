package com.bookofbrilliantthings.spidron.sandbox;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SpidronParameter
{
	String defaultClass();
	boolean required() default false;
}

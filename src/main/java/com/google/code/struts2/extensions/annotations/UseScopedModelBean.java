package com.google.code.struts2.extensions.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface UseScopedModelBean {
	final static String SESSION = "session";
	final static String REQUEST = "request";
	Class beanClass();
	String beanScope() default REQUEST;
	String beanName();
}

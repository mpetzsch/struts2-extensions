package com.google.code.struts2.extensions.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface CleanScopedModelBean {
	String beanName();
	boolean reserve() default false;
	boolean runClean() default false;
}

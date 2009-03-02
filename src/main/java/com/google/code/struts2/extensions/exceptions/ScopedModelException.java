/**
 * 
 */
package com.google.code.struts2.extensions.exceptions;

/**
 * @author MPETZSCH
 *
 */
@SuppressWarnings("serial")
public class ScopedModelException extends Exception {
	public ScopedModelException(String msg){
		super(msg);
	}
	public ScopedModelException(Throwable cause){
		super(cause);
	}
}

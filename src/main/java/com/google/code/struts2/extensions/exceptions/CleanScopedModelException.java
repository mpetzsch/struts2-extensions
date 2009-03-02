/**
 * 
 */
package com.google.code.struts2.extensions.exceptions;

/**
 * @author MPETZSCH
 *
 */
public class CleanScopedModelException extends Exception {

	/**
	 * @param err
	 */
	public CleanScopedModelException(String err) {
		super(err);
	}

	/**
	 * @param e
	 */
	public CleanScopedModelException(Exception e) {
		super(e);
	}
}

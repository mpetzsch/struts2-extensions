/**
 * HomeFirstEntryInterceptor.java
 */
package com.google.code.struts2.extensions.interceptors;

import java.util.HashSet;
import java.util.Map;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.interceptor.Interceptor;

@SuppressWarnings("serial")
public class HomeFirstEntryInterceptor implements Interceptor
{
	@Inject @WebUIFrameworkLoggerATN
	private LoggerIF logger;
	
	
	private static final String OVERRIDE = "homeFirstEntryOverride";
	private HashSet<String> ALLOWED_ENTRIES = new HashSet<String>();
	
	/* (non-Javadoc)
	 * @see com.opensymphony.xwork2.interceptor.Interceptor#destroy()
	 */
	public void destroy()
	{
		logger.debug(getClass(), "destroy()");
	}

	/* (non-Javadoc)
	 * @see com.opensymphony.xwork2.interceptor.Interceptor#init()
	 */
	public void init()
	{
		logger.debug(getClass(), "init(); allowed entry points are ", ALLOWED_ENTRIES);
	}
	
	public void setAllowedEntries(String allowedEntries)
	{
		logger.debug(getClass(), "setAllowedEntries=", allowedEntries);
		String[] strs = allowedEntries.split(",");
		for (String str : strs)
		{
			ALLOWED_ENTRIES.add(str.trim());
		}
		logger.debug(getClass(), "allowedEntries=", ALLOWED_ENTRIES);
	}

	/* (non-Javadoc)
	 * @see com.opensymphony.xwork2.interceptor.Interceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
	 */
	public String intercept(ActionInvocation invocation) throws Exception
	{
		Map<String, Object> httpSessionMap = invocation.getInvocationContext().getSession();
		Map<String, Object> httpParametersMap = invocation.getInvocationContext().getParameters();
		logger.debug(getClass(), "intercept() for action ", invocation.getAction().getClass().getSimpleName(), " allowedEntries=", ALLOWED_ENTRIES);
		
		// if we aren't accessing an allowed action then check we have done before
		if (!httpParametersMap.containsKey(OVERRIDE) && !ALLOWED_ENTRIES.contains(invocation.getAction().getClass().getSimpleName()))
		{
			logger.debug(getClass(), "intercept() Not going to Any Allowed; checking for Key");
			if (httpSessionMap.containsKey(WebConstants.HOME_FIRST_KEY))
			{
				logger.debug(getClass(), "intercept() Key present; allowing to continue");
				return invocation.invoke();
			} else
			{
				logger.debug(getClass(), "intercept() Key NOT present; sending to Home");
				httpSessionMap.put(WebConstants.HOME_FIRST_KEY, WebConstants.HOME_FIRST_VALUE);
				return BaseAction.HOME;
			}
		} else
		{
			logger.debug(getClass(), "intercept() Accessing allowed Action; setting Key");
			httpSessionMap.put(WebConstants.HOME_FIRST_KEY, WebConstants.HOME_FIRST_VALUE);
			return invocation.invoke();
		}
	}
}

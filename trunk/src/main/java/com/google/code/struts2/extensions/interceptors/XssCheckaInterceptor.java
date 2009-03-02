package com.google.code.struts2.extensions.interceptors;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ValidationAware;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.interceptor.Interceptor;

@SuppressWarnings("serial")
public class XssCheckaInterceptor implements Interceptor {
	@Inject
	@WebUIFrameworkLoggerATN
	private LoggerIF logger;

	@Inject
	private MaliciousStringFilterIF maliciousStringFilter;

	private final HashSet<String> EXCLUDE_ACTIONS = new HashSet<String>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opensymphony.xwork2.interceptor.Interceptor#destroy()
	 */
	public void destroy() {
		logger.debug(getClass(), "destroy()");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opensymphony.xwork2.interceptor.Interceptor#init()
	 */
	public void init() {
		logger.debug(getClass(), "init(); allowed entry points are ",
				EXCLUDE_ACTIONS);
	}

	public void setExcludeActions(String allowedEntries) {
		logger.debug(getClass(), "setExcludeActions=", allowedEntries);
		String[] strs = allowedEntries.split(",");
		for (String str : strs) {
			EXCLUDE_ACTIONS.add(str.trim());
		}
		logger.debug(getClass(), "excludeActions=", EXCLUDE_ACTIONS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opensymphony.xwork2.interceptor.Interceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
	 */
	public String intercept(ActionInvocation invocation) throws Exception {

		logger.debug(getClass(), "intercept() for action ", invocation
				.getAction().getClass().getSimpleName(), " excludeActions=",
				EXCLUDE_ACTIONS);

		// if we aren't accessing an exclude action then check parameters
		if (!EXCLUDE_ACTIONS.contains(invocation.getAction().getClass()
				.getSimpleName())) {
			Map params = invocation.getInvocationContext().getParameters();
			logger.trace(getClass(),
					"intercept() Perform Action XSS check on parameters : ",
					params);

			// Loop through all parameters
			Object action = invocation.getAction();

			Iterator paramValueIt = params.entrySet().iterator();
			while (paramValueIt.hasNext()) {
				Map.Entry param = (Map.Entry) paramValueIt.next();
				Object values = param.getValue();
				if (null != values) {

					// Check All values in the parameter value array
					if (values instanceof String[]) {
						logger
								.trace(
										getClass(),
										"intercept() checking for malicious string for action ",
										invocation.getAction().getClass()
												.getSimpleName(),
										" parameter values : ", values);

						for (int i = 0; i < ((String[]) values).length; i++) {
							checkMalicious(param.getKey(),
									((String[]) values)[i], action);
						}
					} else if (values instanceof String) {
						logger
								.trace(
										getClass(),
										"intercept() checking for malicious string for action ",
										invocation.getAction().getClass()
												.getSimpleName(),
										" parameter values : ", values);
						checkMalicious(param.getKey(), (String) values, action);
					}

				}

			}

		} else {
			logger.debug(getClass(),
					"intercept() Ignore allowed Action no XSS check required");
		}

		return invocation.invoke();
	}

	private void checkMalicious(Object paramKey, String paramValue,
			Object action) {
		try {
			logger
					.trace(
							getClass(),
							"checkMalicious() checking for malicious string for action ",
							action.getClass().getSimpleName(),
							" parameter values : ", paramValue);

			maliciousStringFilter.checkMalicious(paramValue);

		} catch (MaliciousStringException mse) {
			// Create error as soon as discovered

			if (action instanceof ValidationAware) {
				logger.error(getClass(), mse,
						"intercept() MaliciousStringException : ", mse
								.getMessage(), " action is ValidationAware : ",
						action.getClass().getSimpleName(), " parameter : ",
						paramKey, " value : ", paramValue);
				((ValidationAware) action)
						.addActionError("Malicious data detected in input data :"
								+ paramValue + " please re-enter");
			} else {
				logger.error(getClass(), mse,
						"intercept() MaliciousStringException : ", mse
								.getMessage(),
						" action is not ValidationAware : ", action.getClass()
								.getSimpleName(), " parameter : ", paramKey,
						" value : ", paramValue);
			}

		}

	}
}

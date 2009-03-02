package com.google.code.struts2.extensions.interceptors;

import java.util.Map;

import com.google.code.struts2.extensions.annotations.UseScopedModelBean;
import com.google.code.struts2.extensions.annotations.UseScopedModelBeans;
import com.google.code.struts2.extensions.exceptions.ScopedModelException;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.interceptor.Interceptor;

@SuppressWarnings("serial")
public class ScopedModelInterceptor implements Interceptor
{

	@Inject
	@WebUIFrameworkLoggerATN
	private LoggerIF logger;

	public void destroy()
	{
	}

	public void init()
	{
	}

	public String intercept(ActionInvocation arg0) throws Exception
	{
		try
		{
			logger.debug(this.getClass(),
					"ScopedModelInterceptor - examining: ", arg0.getAction()
							.getClass().getSimpleName());

			if (arg0.getAction().getClass().isAnnotationPresent(
					UseScopedModelBeans.class))
			{
				logger.debug(this.getClass(), "UseScopedModelBeans: ", arg0
						.getAction().getClass().getCanonicalName());

				UseScopedModelBeans multiUse = arg0.getAction().getClass()
						.getAnnotation(UseScopedModelBeans.class);

				for (UseScopedModelBean useBean : multiUse.value())
				{
					injectModelBean(useBean, arg0);
				}
			} else if (arg0.getAction().getClass().isAnnotationPresent(
					UseScopedModelBean.class))
			{
				logger.debug(this.getClass(), "UseScopedModelBean: ", arg0
						.getAction().getClass().getCanonicalName());
				injectModelBean(arg0.getAction().getClass().getAnnotation(
						UseScopedModelBean.class), arg0);
			}
		} catch (Throwable e)
		{
			logger.error(this.getClass(), e,
					"Exception in ScopedModelInterceptor");
			throw new ScopedModelException(e);
		}
		return arg0.invoke();
	}

	private void injectModelBean(UseScopedModelBean useBean,
			ActionInvocation arg0) throws Exception
	{
		// get javabeans std. method name
		if (useBean.beanName() == null || useBean.beanName().length() < 1)
		{
			String err = produceError(arg0,
					"beanName must be specified as a String on UseScopedModelBean");
			logger.error(this.getClass(), err);
			throw new ScopedModelException(err);
		} else
		{
			logger.debug(this.getClass(), "Injecting ", useBean.beanName(),
					" of type ", useBean.beanClass(), " into ", arg0
							.getAction().getClass().getCanonicalName());

			String methodName = convertCamelCase(useBean.beanName());
			Map<String, Object> context = null;
			if (useBean.beanScope().equals(UseScopedModelBean.SESSION))
			{
				context = arg0.getInvocationContext().getSession();
			} else if (useBean.beanScope().equals(UseScopedModelBean.REQUEST))
			{
				context = (Map) arg0.getStack().findValue("#request");
			}

			Object beanInstance = null;
			if (context.get(useBean.beanName()) != null)
			{
				beanInstance = context.get(useBean.beanName());

				// check instance of expected type
				if (!useBean.beanClass().isInstance(beanInstance))
				{
					String err = produceError(
							arg0,
							"Type Conflict: Bean "
									+ useBean.beanName()
									+ " found in scope "
									+ useBean.beanScope()
									+ " is not of expected type "
									+ useBean.beanClass()
									+ " but is of type "
									+ beanInstance.getClass()
									+ ".  Ensure that different bean classes are not sharing the same beanName within the same scope");
					logger.error(getClass(), err);
					throw new ScopedModelException(err);
				}
			} else
			{
				beanInstance = useBean.beanClass().newInstance();
			}

			// set on action and scope
			try
			{
				arg0.getAction().getClass().getMethod("set" + methodName,
						useBean.beanClass()).invoke(arg0.getAction(),
						beanInstance);
			} catch (NoSuchMethodException nsme)
			{
				String err = produceError(arg0,
						"Ensure a method is present on this action for injection of "
								+ useBean.beanName() + " of type "
								+ useBean.beanClass() + ". [ set"
								+ convertCamelCase(useBean.beanName()) + "("
								+ useBean.beanClass().getCanonicalName()
								+ ") ]");
				logger.error(this.getClass(), err);
				throw new ScopedModelException(err);
			}
			context.put(useBean.beanName(), beanInstance);
		}
	}

	private String convertCamelCase(String str)
	{
		return str.substring(0, 1).toUpperCase()
				+ str.substring(1, str.length());
	}

	/**
	 * @param arg0
	 * @param string
	 */
	private String produceError(ActionInvocation arg0, String string)
	{
		return "Error from " + this.getClass().getCanonicalName()
				+ ": [action=" + arg0.getAction().getClass().getCanonicalName()
				+ "] " + string;
	}
}

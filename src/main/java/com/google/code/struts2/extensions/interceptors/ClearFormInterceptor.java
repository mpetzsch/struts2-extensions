package com.google.code.struts2.extensions.interceptors;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.code.struts2.extensions.annotations.CleanScopedModelBean;
import com.google.code.struts2.extensions.annotations.CleanScopedModelBeans;
import com.google.code.struts2.extensions.annotations.UseScopedModelBean;
import com.google.code.struts2.extensions.annotations.UseScopedModelBeans;
import com.google.code.struts2.extensions.exceptions.CleanScopedModelException;
import com.google.code.struts2.extensions.interfaces.Cleanable;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.interceptor.Interceptor;

@SuppressWarnings("serial")
public class ClearFormInterceptor implements Interceptor {
	@Inject
	@WebUIFrameworkLoggerATN
	private LoggerIF logger;
	
	private Set<String> includedMethods = new HashSet<String>();

	public void destroy() {
	}

	public void init() {
		
	}
	
	public void setIncludeMethods(String methods)
	{
		logger.debug(getClass(), "setIncludeMethods=", methods);
		String[] strs = methods.split(",");
		for (String str : strs)
		{
			includedMethods.add(str.trim());
		}
		logger.debug(getClass(), "includedMethods=", includedMethods);
	}

	public String intercept(ActionInvocation invocation) throws Exception {
		logger.debug(this.getClass(), "clearForm: "
				, invocation.getAction().getClass().getSimpleName(), " includedMethods=", includedMethods);

		String actionMethod = invocation.getProxy().getMethod(); 
		
		logger.debug(this.getClass(), "clearForm; method is: ", actionMethod);
		
		
		if (includedMethods.contains(actionMethod))
		{
			logger.debug(getClass(), "clearForm - interceptor active; ", actionMethod, " is included - cleaning and invoking action only");
			runCleaning(invocation);
			return invocation.invokeActionOnly();
		} else
		{
			logger.debug(getClass(), "clearForm - interceptor NOT active; ", actionMethod, " is not included");
			return invocation.invoke();
		}
	}
	
	private void runCleaning(ActionInvocation invocation) throws Exception
	{
		// see if action method is annotated with cleanup
		String methodName = invocation.getProxy().getMethod();
		Method actionMethod = invocation.getAction().getClass().getMethod(
				methodName, new Class[] {});

		if (actionMethod.isAnnotationPresent(CleanScopedModelBean.class)
				|| actionMethod
						.isAnnotationPresent(CleanScopedModelBeans.class)) {
			logger.debug(this.getClass(), "Cleanup specified on method "
					, actionMethod.getName(), " checking for beans");
			// check there are some used beans to cleanup
			Hashtable<String, UseScopedModelBean> useBeans = getUseBeans(invocation);
			logger.debug(this.getClass(), "There are ", useBeans.size()
					, " UseScopedModelBeans");
			// get the list of beans to clean up
			List<CleanScopedModelBean> cleanBeans = getCleanBeans(invocation,
					actionMethod);
			logger.debug(this.getClass(), "There are ", cleanBeans.size()
					, " CleanScopedModelBeans");

			for (CleanScopedModelBean cleanup : cleanBeans) {
				logger.debug(this.getClass(), "Cleaning bean "
						, cleanup.beanName() , " from action "
						, invocation.getAction().getClass().getName());
				cleanBean(cleanup, invocation, useBeans);
			}
		}
	}

	/**
	 * @param cleanup
	 * @param invocation
	 * @param useBeans
	 */
	private void cleanBean(CleanScopedModelBean cleanup,
			ActionInvocation invocation,
			Hashtable<String, UseScopedModelBean> useBeans)
			throws CleanScopedModelException {

		// get the used bean by name on the clean annotation
		UseScopedModelBean user = useBeans.get(cleanup.beanName());
		if (user == null) {
			String err = produceError(invocation,
					"Could not find a UseScopedModelBean with beanName "
							+ cleanup.beanName()
							+ " to match CleanScopedModelBean on action "
							+ invocation.getAction().getClass().getName());
			logger.error(this.getClass(), err);
			throw new CleanScopedModelException(err);
		} else {
			// try to get method to set on action
			String setMethodName = "set" + convertCamelCase(user.beanName());
			String getMethodName = "get" + convertCamelCase(user.beanName());
			boolean reserve = cleanup.reserve();
			boolean runClean = cleanup.runClean();
			Class beanClass = user.beanClass();
			String beanScope = user.beanScope();
			String beanName = user.beanName();

			// get relevant context for cleanup
			Map<String, Object> context = null;
			if (user.beanScope().equals(UseScopedModelBean.SESSION)) {
				context = invocation.getInvocationContext().getSession();
			} else if (user.beanScope().equals(UseScopedModelBean.REQUEST)) {
				context = (Map) invocation.getStack().findValue("#request");
			}

			logger.debug(this.getClass(), "Cleaning " , beanClass
					, " from scope " , beanScope , "?reserve=" , reserve
					, "&clean=" , runClean);

			// get the current bean
			Method setMethod = null;
			Method getMethod = null;
			try {
				logger.debug(this.getClass(), "Retrieving method "
						, getMethodName, " from action "
						, invocation.getAction().getClass().getName());
				getMethod = invocation.getAction().getClass().getMethod(
						getMethodName, null);
				logger.debug(this.getClass(), "Retrieving method "
						, setMethodName, " from action "
						, invocation.getAction().getClass().getName());
				setMethod = invocation.getAction().getClass().getMethod(
						setMethodName, new Class[] { beanClass });
			} catch (NoSuchMethodException nsme) {
				String err = produceError(
						invocation,
						"Ensure the Set/Get methods are present on this action for cleanup injection of "
								+ user.beanName()
								+ " of type "
								+ user.beanClass()
								+ ". [ set/get"
								+ convertCamelCase(user.beanName())
								+ "("
								+ user.beanClass().getCanonicalName() + ") ]");
				logger.error(this.getClass(), err);
				throw new CleanScopedModelException(err);
			}

			// wrap in exception as cannot handle diff exceptions differently
			// anyway
			Object currentBean = null;
			try {
				logger
						.debug(this.getClass(),
								"Retrieving current bean using getMethod: "
										, getMethod);
				// get current bean
				currentBean = getMethod.invoke(invocation.getAction(),
						new Class[] {});
			} catch (Exception e) {
				String err = produceError(invocation, e.toString());
				logger.error(this.getClass(), err);
				throw new CleanScopedModelException(e);
			}

			// remove from context OR clean
			if (runClean) {
				logger.debug(this.getClass(),
						"runClean specified - attempting to clean "
								, currentBean);
				if (currentBean instanceof Cleanable) {
					((Cleanable) currentBean).clean();
					logger.debug(this.getClass(),
							"runClean specified and cleanable - cleaning "
									, currentBean);
				} else {
					String err = produceError(invocation, currentBean
							+ " does not implement " + Cleanable.class
							+ " but is marked for clean");
					logger.error(this.getClass(), err);
					throw new CleanScopedModelException(err);
				}
			} else {
				logger.debug(this.getClass(), "Creating new instance of "
						, beanClass);
				try {
					Object newBean = beanClass.newInstance();
					logger.debug(this.getClass(), "Placing new instance in "
							, beanScope);
					context.put(beanName, newBean);
					if (!reserve) {
						setMethod.invoke(invocation.getAction(), newBean);
					} else {
						logger.debug(this.getClass(),
								"Bean reserved on action " , beanName);
					}
				} catch (Exception e) {
					String err = produceError(invocation, e.toString());
					logger.error(this.getClass(), err);
					throw new CleanScopedModelException(e);
				}
			}
		}
	}

	/**
	 * @param arg0
	 * @return
	 */
	private List<CleanScopedModelBean> getCleanBeans(ActionInvocation arg0,
			Method actionMethod) {
		ArrayList<CleanScopedModelBean> cleanBeans = new ArrayList<CleanScopedModelBean>();

		// find beans which can be cleaned up
		if (actionMethod.isAnnotationPresent(CleanScopedModelBean.class)) {
			// single bean to cleanup
			cleanBeans.add(actionMethod
					.getAnnotation(CleanScopedModelBean.class));
		} else if (actionMethod
				.isAnnotationPresent(CleanScopedModelBeans.class)) {
			// multiple beans to clean up
			CleanScopedModelBeans cleanSBeans = actionMethod
					.getAnnotation(CleanScopedModelBeans.class);
			for (CleanScopedModelBean cb : cleanSBeans.value()) {
				cleanBeans.add(cb);
			}
		}
		logger.debug(this.getClass(), arg0.getAction().getClass().getCanonicalName() , ":"
				, actionMethod.getName() , " requires cleanup of "
				, cleanBeans.size() , " beans");
		return cleanBeans;
	}

	/**
	 * @param arg0
	 * @return
	 */
	private Hashtable<String, UseScopedModelBean> getUseBeans(
			ActionInvocation arg0) {
		Hashtable<String, UseScopedModelBean> useBeans = new Hashtable<String, UseScopedModelBean>();

		// find beans which can be cleaned up
		if (arg0.getAction().getClass().isAnnotationPresent(
				UseScopedModelBean.class)) {
			// single bean to cleanup
			useBeans.put(arg0.getAction().getClass().getAnnotation(
					UseScopedModelBean.class).beanName(), arg0.getAction()
					.getClass().getAnnotation(UseScopedModelBean.class));
		} else if (arg0.getAction().getClass().isAnnotationPresent(
				UseScopedModelBeans.class)) {
			// multiple beans to clean up
			UseScopedModelBeans useSBeans = arg0.getAction().getClass()
					.getAnnotation(UseScopedModelBeans.class);
			for (UseScopedModelBean ub : useSBeans.value()) {
				useBeans.put(ub.beanName(), ub);
			}
		}
		logger.debug(this.getClass(), arg0.getAction().getClass().getCanonicalName()
				, " uses " , useBeans.size() , " injected beans");
		return useBeans;
	}

	private String convertCamelCase(String str) {
		return str.substring(0, 1).toUpperCase()
				+ str.substring(1, str.length());
	}

	/**
	 * @param arg0
	 * @param string
	 */
	private String produceError(ActionInvocation arg0, String string) {
		return "Error from " + this.getClass().getCanonicalName()
				+ ": [action=" + arg0.getAction().getClass().getCanonicalName()
				+ "] " + string;
	}
}

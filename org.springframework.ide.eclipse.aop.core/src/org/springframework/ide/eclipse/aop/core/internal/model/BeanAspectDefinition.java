/*******************************************************************************
 * Copyright (c) 2005, 2007 Spring IDE Developers
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Spring IDE Developers - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.aop.core.internal.model;

import java.lang.reflect.Method;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.springframework.beans.BeanUtils;
import org.springframework.ide.eclipse.aop.core.model.IAopReference;
import org.springframework.ide.eclipse.aop.core.model.IAspectDefinition;
import org.springframework.ide.eclipse.aop.core.model.IAopReference.ADVICE_TYPES;
import org.springframework.ide.eclipse.core.java.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christian Dupuis
 * @since 2.0
 */
public class BeanAspectDefinition implements IAspectDefinition, IAdaptable,
		IPersistableElement {

	protected String adivceMethodName;

	protected String[] adivceMethodParameterTypes = new String[0];

	protected String[] argNames;

	protected String aspectClassName;

	protected int aspectLineNumber = -1;

	protected String aspectName = "";

	protected IResource file;

	protected boolean isProxyTargetClass = false;

	protected String pointcutExpressionString = null;

	protected String returning;

	protected String throwing;

	protected IAopReference.ADVICE_TYPES type;

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BeanAspectDefinition) {
			BeanAspectDefinition other = (BeanAspectDefinition) obj;
			return other.getAspectLineNumber() == getAspectLineNumber()
					&& other.getAspectName().equals(aspectName)
					&& other.getType().equals(type);
		}
		return false;
	}

	public Object getAdapter(Class adapter) {
		if (adapter.equals(IPersistableElement.class)) {
			return this;
		}
		return null;
	}

	public Method getAdviceMethod() {
		try {
			Class<?> aspectClass = ClassUtils.loadClass(this.aspectClassName);
			Method method = BeanUtils.resolveSignature(this.adivceMethodName,
					aspectClass);
			return method;
		}
		catch (ClassNotFoundException e) {
			return null;
		}
	}

	public String getAdviceMethodName() {
		return adivceMethodName;
	}

	public String[] getAdviceMethodParameterTypes() {
		return this.adivceMethodParameterTypes;
	}

	public String[] getArgNames() {
		return argNames;
	}

	public String getAspectClassName() {
		return aspectClassName;
	}

	public int getAspectLineNumber() {
		return aspectLineNumber;
	}

	public String getAspectName() {
		return aspectName;
	}

	public String getFactoryId() {
		return BeanAspectDefinitionElementFactory.FACTORY_ID;
	}

	public String getPointcutExpression() {
		return this.pointcutExpressionString;
	}

	public IResource getResource() {
		return file;
	}

	public String getReturning() {
		return returning;
	}

	public String getThrowing() {
		return throwing;
	}

	public IAopReference.ADVICE_TYPES getType() {
		return type;
	}

	@Override
	public int hashCode() {
		int hc = aspectName.hashCode();
		hc = 23 * hc + type.hashCode();
		hc = 25 * hc + aspectLineNumber;
		return hc;
	}

	public boolean isProxyTargetClass() {
		return this.isProxyTargetClass;
	}

	public void saveState(IMemento memento) {
		memento
				.putString(
						BeanAspectDefinitionElementFactory.ADVICE_METHOD_NAME_ATTRIBUTE,
						this.adivceMethodName);
		memento.putString(
				BeanAspectDefinitionElementFactory.ADVICE_CLASS_NAME_ATTRIBUTE,
				this.aspectClassName);
		if (this.adivceMethodParameterTypes != null
				&& this.adivceMethodParameterTypes.length > 0) {
			memento
					.putString(
							BeanAspectDefinitionElementFactory.ADIVCE_METHOD_PARAMETER_TYPES_ATTRIBUTE,
							StringUtils
									.arrayToCommaDelimitedString(this.adivceMethodParameterTypes));
		}
		memento.putString(
				BeanAspectDefinitionElementFactory.ASPECT_NAME_ATTRIBUTE,
				this.aspectName);
		memento
				.putString(
						BeanAspectDefinitionElementFactory.POINTCUT_EXPRESSION_ATTRIBUTE,
						this.pointcutExpressionString);
		memento.putString(
				BeanAspectDefinitionElementFactory.RETURNING_ATTRIBUTE,
				this.returning);
		memento.putString(
				BeanAspectDefinitionElementFactory.THROWING_ATTRIBUTE,
				this.throwing);
		if (this.argNames != null && this.argNames.length > 0) {
			memento.putString(
					BeanAspectDefinitionElementFactory.ARG_NAMES_ATTRIBUTE,
					StringUtils.arrayToCommaDelimitedString(this.argNames));
		}
		memento
				.putInteger(
						BeanAspectDefinitionElementFactory.ASPECT_LINE_NUMBER_ATTRIBUTE,
						this.aspectLineNumber);
		memento.putString(BeanAspectDefinitionElementFactory.FILE_ATTRIBUTE,
				this.file.getFullPath().toString());
		memento
				.putString(
						BeanAspectDefinitionElementFactory.PROXY_TARGET_CLASS_ATTRIBUTE,
						Boolean.toString(this.isProxyTargetClass));
		memento.putString(
				BeanAspectDefinitionElementFactory.ADVICE_TYPE_ATTRIBUTE,
				this.type.toString());
	}

	public void setAdviceMethodName(String adivceMethodName) {
		this.adivceMethodName = adivceMethodName;
	}

	public void setAdviceMethodParameterTypes(String[] params) {
		this.adivceMethodParameterTypes = params;
	}

	public void setArgNames(String[] argNames) {
		this.argNames = argNames;
	}

	public void setAspectClassName(String aspectClassName) {
		this.aspectClassName = aspectClassName;
	}

	public void setAspectLineNumber(int aspectLineNumber) {
		this.aspectLineNumber = aspectLineNumber;
	}

	public void setAspectName(String aspectName) {
		if (!StringUtils.hasText(aspectName)) {
			this.aspectName = "anonymous aspect";
		}
		else {
			this.aspectName = aspectName;
		}
	}

	public void setPointcutExpression(String expression) {
		this.pointcutExpressionString = expression;
	}

	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.isProxyTargetClass = proxyTargetClass;
	}

	public void setResource(IResource file) {
		this.file = file;
	}

	public void setReturning(String returning) {
		this.returning = returning;
	}

	public void setThrowing(String throwing) {
		this.throwing = throwing;
	}

	public void setType(IAopReference.ADVICE_TYPES type) {
		this.type = type;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Aspect definition");
		if (this.file != null) {
			buf.append(" [");
			buf.append(this.file.getFullPath().toFile());
			buf.append(":");
			buf.append(getAspectLineNumber());
			buf.append("]");
		}
		buf.append(" advise type [");
		ADVICE_TYPES type = getType();
		if (type == ADVICE_TYPES.AFTER) {
			buf.append("after");
		}
		else if (type == ADVICE_TYPES.AFTER_RETURNING) {
			buf.append("after-returning");
		}
		else if (type == ADVICE_TYPES.AFTER_THROWING) {
			buf.append("after-throwing");
		}
		else if (type == ADVICE_TYPES.BEFORE) {
			buf.append("before");
		}
		else if (type == ADVICE_TYPES.AROUND) {
			buf.append("after");
		}
		else if (type == ADVICE_TYPES.DECLARE_PARENTS) {
			buf.append("delcare parents");
		}
		buf.append("] advise [");
		buf.append(getAspectClassName());
		if (type != ADVICE_TYPES.DECLARE_PARENTS) {
			buf.append(".");
			buf.append(getAdviceMethodName());
		}
		buf.append("]");
		return buf.toString();
	}
}

/**
 *    Copyright 2009-2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Clinton Begin
 * 这个类是被Interceptor.intercept(Invocation)调用
 * 我们通过这个类可以获知,封装了对象,方法,参数
 * 以及调用method的操作
 */
public class Invocation {

	/**
	 * 目标对象
	 */
	private final Object target;
	/**
	 * 目标对象的方法
	 */
	private final Method method;
	/**
	 * 目标对象的方法的参数
	 */
	private final Object[] args;

	public Invocation(Object target, Method method, Object[] args) {
		this.target = target;
		this.method = method;
		this.args = args;
	}

	public Object getTarget() {
		return target;
	}

	public Method getMethod() {
		return method;
	}

	public Object[] getArgs() {
		return args;
	}

	/**
	 * 执行目标对象的方法
	 * @return
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	public Object proceed() throws InvocationTargetException, IllegalAccessException {
		return method.invoke(target, args);
	}

}

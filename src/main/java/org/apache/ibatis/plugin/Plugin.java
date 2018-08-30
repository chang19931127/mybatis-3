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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * @author Clinton Begin
 * InvocationHandler这个类熟悉把,jdk动态代理。
 * 很显然了,这个类就是代理处理类了,形象的称呼为插件
 * 插件去拦截么
 */
public class Plugin implements InvocationHandler {

	/**
	 * 目标对象
	 */
	private final Object target;
	/**
	 * 拦截器
	 */
	private final Interceptor interceptor;
	/**
	 * 这里面应该存放 signature集合,就是标志要拦截的四大对象
	 * 需要Signature 和方法参数,搭配出来就是   需要拦截的类的方法了
	 * type ->(method,args)
	 */
	private final Map<Class<?>, Set<Method>> signatureMap;

	private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
		this.target = target;
		this.interceptor = interceptor;
		// signatureMap是构造进来的
		this.signatureMap = signatureMap;
	}

	/**
	 * 获得代理的对象把
	 * 需要传入被代理对象和interceptor烂机器
	 * @param target
	 * @param interceptor
	 * @return
	 */
	public static Object wrap(Object target, Interceptor interceptor) {
		// 通过拦截器找到 signature
		Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
		// 获得目标的类型
		Class<?> type = target.getClass();
		// 获得这个type类 在signatureMap中的所有接口
		Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
		if (interfaces.length > 0) {
			// 多接口代理,并传入Plugin对象 生成代理类
			return Proxy.newProxyInstance(
					type.getClassLoader(),
					interfaces,
					new Plugin(target, interceptor, signatureMap));
		}
		return target;
	}

	/**
	 * 复写的核心方法,Proxy也是通过这个方法来handler
	 * @param proxy
	 * @param method
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			// 拿到method 的声明类型
			Set<Method> methods = signatureMap.get(method.getDeclaringClass());
			if (methods != null && methods.contains(method)) {
				// 拦截器.intercept   Invocation(这里直接封装进去了)
				return interceptor.intercept(new Invocation(target, method, args));
			}
			return method.invoke(target, args);
		} catch (Exception e) {
			throw ExceptionUtil.unwrapThrowable(e);
		}
	}

	private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
		// Intercepts 注解
		Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
		// issue #251 null 就返回异常,
		if (interceptsAnnotation == null) {
			throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
		}
		// 通过Intercepts 找到 Signature
		Signature[] sigs = interceptsAnnotation.value();
		Map<Class<?>, Set<Method>> signatureMap = new HashMap<Class<?>, Set<Method>>();
		for (Signature sig : sigs) {
			// 然后将Signature 中的type四大类型作为key存储 value 暂且存引用methods
			Set<Method> methods = signatureMap.get(sig.type());
			if (methods == null) {
				methods = new HashSet<Method>();
				signatureMap.put(sig.type(), methods);
			}
			try {
				// 然后通过type 的方法 参数 获得method 在添加到methods中
				Method method = sig.type().getMethod(sig.method(), sig.args());
				methods.add(method);
				// 为什么这么做 就是 Map<Class<?>, Set<Method>> 这样的返回结果   一个key可能对应的value 集合有多个值
			} catch (NoSuchMethodException e) {
				throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
			}
		}
		// Signature 中 type中 找出对应的method和args的method 组成      key=value -> type ->(method,args)
		return signatureMap;
	}

	private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
		Set<Class<?>> interfaces = new HashSet<Class<?>>();
		while (type != null) {
			for (Class<?> c : type.getInterfaces()) {
				if (signatureMap.containsKey(c)) {
					// 获得 type的接口 在signatureMap中的接口取出来
					interfaces.add(c);
				}
			}
			type = type.getSuperclass();
		}
		// 集合 toArrays
		return interfaces.toArray(new Class<?>[interfaces.size()]);
	}

}

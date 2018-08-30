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
package org.apache.ibatis.binding;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.ibatis.lang.UsesJava7;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * mybatis 中最重要的地方 通过接口jdk动态代理来进行 实现类.并且对方法进行实现
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

	private static final long serialVersionUID = -6424540398559729838L;
	/**
	 * mybatis关键对象SqlSession
	 */
	private final SqlSession sqlSession;
	/**
	 * 单个Mapper的接口class
	 */
	private final Class<T> mapperInterface;
	/**
	 * 单个Mapper的所有方法
	 */
	private final Map<Method, MapperMethod> methodCache;

	public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
		this.sqlSession = sqlSession;
		this.mapperInterface = mapperInterface;
		this.methodCache = methodCache;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 代理后执行的方法
		try {
			if (Object.class.equals(method.getDeclaringClass())) {
				// 如果方法是被Object 声明的 就直接调用
				return method.invoke(this, args);
			} else if (isDefaultMethod(method)) {
				// 是默认方法直接调用
				return invokeDefaultMethod(proxy, method, args);
			}
		} catch (Throwable t) {
			throw ExceptionUtil.unwrapThrowable(t);
		}
		// 然后获取到MapperMethod 然后才会去执行操作
		final MapperMethod mapperMethod = cachedMapperMethod(method);
		return mapperMethod.execute(sqlSession, args);
	}

	/**
	 * 直接通过 Method 获取 MapperMethod
	 * @param method
	 * @return
	 */
	private MapperMethod cachedMapperMethod(Method method) {
		MapperMethod mapperMethod = methodCache.get(method);
		if (mapperMethod == null) {
			// 获取MapperMethod 对象 并添加到缓存
			mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
			// 这里去对methodCache 进行缓存
			methodCache.put(method, mapperMethod);
		}
		return mapperMethod;
	}

	@UsesJava7
	private Object invokeDefaultMethod(Object proxy, Method method, Object[] args)
			throws Throwable {
		final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
				.getDeclaredConstructor(Class.class, int.class);
		if (!constructor.isAccessible()) {
			constructor.setAccessible(true);
		}
		final Class<?> declaringClass = method.getDeclaringClass();
		return constructor
				.newInstance(declaringClass,
						MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
								| MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC)
				.unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
	}

	/**
	 * Backport of java.lang.reflect.Method#isDefault()
	 * jdk 1.8 提供了默认方法 这里就是判断是否是默认方法
	 */
	private boolean isDefaultMethod(Method method) {
		return (method.getModifiers()
				& (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC
				&& method.getDeclaringClass().isInterface();
	}
}

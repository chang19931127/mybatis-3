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
package org.apache.ibatis.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author Clinton Begin
 * 关于异常操作的工具类
 */
public class ExceptionUtil {

	private ExceptionUtil() {
		// Prevent Instantiation
	}

	/**
	 * 对异常信息进行拆包把
	 * @param wrapped 异常的父类
	 * @return 最终未包装的异常
	 */
	public static Throwable unwrapThrowable(Throwable wrapped) {
		Throwable unwrapped = wrapped;
		while (true) {
			// 如果是InvocationTargetException 或者 UndeclaredThrowableException 异常 jdk中的反射异常
			// 这两个异常都调用的super(msg,null)父类，难道仅仅是为了自己封装异常信息，不抛给父类？
			// 多态,导致父类获得不了,子类却可以
			// 理解下这里是为了什么,包装了一个为检查异常
			// 通过get方法拿出对应的私有属性Throwable
			// 最终返回真是的异常类
			if (unwrapped instanceof InvocationTargetException) {
				// 调用方法的异常,调用的目标有问题
				unwrapped = ((InvocationTargetException) unwrapped).getTargetException();
			} else if (unwrapped instanceof UndeclaredThrowableException) {
				// 调用方法的异常,调用的方法未声明
				unwrapped = ((UndeclaredThrowableException) unwrapped).getUndeclaredThrowable();
			} else {
				return unwrapped;
			}
		}
	}

}

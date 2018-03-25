/**
 * Copyright 2009-2015 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.reflection;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * @author Clinton Begin
 * 自定义异常,我们可以学习下
 * 其实就是继承RuntimeException,然后写几个构造方法,
 * 调用super();带有参数的,提示信息,异常栈信息
 *
 */
public class ReflectionException extends PersistenceException {

	private static final long serialVersionUID = 7642570221267566591L;

	public ReflectionException() {
		super();
	}

	public ReflectionException(String message) {
		super(message);
	}

	public ReflectionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReflectionException(Throwable cause) {
		super(cause);
	}

}

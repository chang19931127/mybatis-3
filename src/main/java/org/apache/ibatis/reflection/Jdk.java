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

import org.apache.ibatis.io.Resources;

/**
 * To check the existence of version dependent classes.
 * 个人理解这个类是,check in版本的
 * 因为Mybatis使用了Parameter和Clock以及Optional这三个1.8的类
 * 通过Resources来进行资源加载可以学一波哦
 */
public class Jdk {

	/**
	 * <code>true</code> if <code>java.lang.reflect.Parameter</code> is available.
	 */
	public static final boolean parameterExists;

	static {
		boolean available = false;
		try {
			// Parameter:1.8提供的关于反射参数的工具类
			// 这个类可以被Method和Constructor获得
			Resources.classForName("java.lang.reflect.Parameter");
			available = true;
		} catch (ClassNotFoundException e) {
			// ignore
		}
		parameterExists = available;
	}

	public static final boolean dateAndTimeApiExists;

	static {
		boolean available = false;
		try {
			// Clock:1.8提供的时间类
			Resources.classForName("java.time.Clock");
			available = true;
		} catch (ClassNotFoundException e) {
			// ignore
		}
		dateAndTimeApiExists = available;
	}

	public static final boolean optionalExists;

	static {
		boolean available = false;
		try {
			// Optional:1.8提供的帮助类
			Resources.classForName("java.util.Optional");
			available = true;
		} catch (ClassNotFoundException e) {
			// ignore
		}
		optionalExists = available;
	}

	private Jdk() {
		super();
	}
}

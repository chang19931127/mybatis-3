/**
 * Copyright 2009-2018 the original author or authors.
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

import org.apache.ibatis.lang.UsesJava8;

import java.util.Optional;

/**
 * OptionalUtil 工具类,使用了java8的api 使用之前我们说过的注解来标识的
 * java8通过包装类,来对传入的对象进行包装,来做一些特殊的操作
 * 通过包装成Optional对象进行操作
 * Optional可以解决循环调用 null的问题
 *
 */
public abstract class OptionalUtil {

	@UsesJava8
	public static Object ofNullable(Object value) {
		// Optional.ofNullable 把obj 包装到Optional.value属性中
		// 其实这个Object是Optional对象
		return Optional.ofNullable(value);
	}

	private OptionalUtil() {
		super();
	}

	public static void main(String[] args) {
		// 这个操作是可以null的情况
		System.out.println(OptionalUtil.ofNullable(null));
		System.out.println(OptionalUtil.ofNullable("optional"));
		// 会在这里提前抛出NPE哈哈,所以请领会ofNullable()的设计
		Optional optional = Optional.of("of");
		System.out.println(optional.get());
	}
}

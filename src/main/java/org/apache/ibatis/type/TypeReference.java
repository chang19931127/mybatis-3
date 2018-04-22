/**
 * Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * References a generic type.
 *
 * @param <T> the referenced type
 * @since 3.1.0
 * @author Simone Tripodi
 *
 * 这个累的功能超级简单,就是一个泛型类,只是内部封装了一个该泛型的Type对象
 */
public abstract class TypeReference<T> {

	private final Type rawType;

	protected TypeReference() {
		rawType = getSuperclassTypeParameter(getClass());
	}

	Type getSuperclassTypeParameter(Class<?> clazz) {
		// 得到这个类的泛型父类
		Type genericSuperclass = clazz.getGenericSuperclass();
		// 如果泛型父类还是类       Type不是Class 是Class的父类哈哈
		if (genericSuperclass instanceof Class) {
			// try to climb up the hierarchy until meet something useful
			// 继续寻找父类知道找到TypeReference 这个父类
			if (TypeReference.class != genericSuperclass) {
				// 递归了
				return getSuperclassTypeParameter(clazz.getSuperclass());
			}

			throw new TypeException("'" + getClass() + "' extends TypeReference but misses the type parameter. "
					+ "Remove the extension or add a type parameter to it.");
		}

		// 知道找到Type然后通过转型成参数化类型拿到第一个真是类型,因为我们泛型就一个
		Type rawType = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
		// TODO remove this when Reflector is fixed to return Types
		// 如果泛型类型还是一个参数化类型就继续走起
		if (rawType instanceof ParameterizedType) {
			rawType = ((ParameterizedType) rawType).getRawType();
		}

		return rawType;
	}

	public final Type getRawType() {
		return rawType;
	}

	@Override
	public String toString() {
		return rawType.toString();
	}

	public static void main(String[] args) {
		class Person<T>{

		}
		class Child extends Person<List<String>>{

		}

		Child person = new Child();
		Type type = person.getClass().getGenericSuperclass();
		System.out.println(type);
		Type rawType = ((ParameterizedType) type).getActualTypeArguments()[0];
		System.out.println(rawType instanceof ParameterizedType);
		Type rawType1 = ((ParameterizedType) rawType).getRawType();
		System.out.println(rawType);
		rawType = ((ParameterizedType) rawType).getActualTypeArguments()[0];
		System.out.println(rawType);

		// 针对反射,获取父类,泛型父类,参数化类型,实际类型,学习下反射API
	}
}

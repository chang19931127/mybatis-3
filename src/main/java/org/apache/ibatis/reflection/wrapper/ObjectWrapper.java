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
package org.apache.ibatis.reflection.wrapper;

import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * @author Clinton Begin
 * 对象包装的接口
 * 感觉还是为了PropertiesToken 那个来进行操作的
 * 就是为了解析出来然后形成对象属性
 */
public interface ObjectWrapper {

	/**
	 * 通过语法来get对象
	 * @param prop  语法对象
	 * @return
	 */
	Object get(PropertyTokenizer prop);

	/**
	 * 通过语法来set对象
	 * @param prop      语法对象
	 * @param value     设置的值
	 */
	void set(PropertyTokenizer prop, Object value);

	/**
	 * 查找属性
	 * @param name                  字符串
	 * @param useCamelCaseMapping   是否驼峰
	 * @return                      字符串
	 */
	String findProperty(String name, boolean useCamelCaseMapping);

	/**
	 * get 方法集合
	 * @return 数组
	 */
	String[] getGetterNames();

	/**
	 * set方法集合
	 * @return 数组
	 */
	String[] getSetterNames();

	/**
	 * 得到set方法的类型
	 * @param name
	 * @return
	 */
	Class<?> getSetterType(String name);

	/**
	 * 得到get方法的类型
	 * @param name
	 * @return
	 */
	Class<?> getGetterType(String name);

	/**
	 * 判断是否有set方法
	 * @param name
	 * @return
	 */
	boolean hasSetter(String name);

	/**
	 * 是否有get方法
	 * @param name
	 * @return
	 */
	boolean hasGetter(String name);

	/**
	 * 构造对象把
	 * @param name
	 * @param prop
	 * @param objectFactory
	 * @return
	 */
	MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

	/**
	 * 是否集合
	 * @return
	 */
	boolean isCollection();

	/**
	 * 添加对象
	 * @param element
	 */
	void add(Object element);

	/**
	 * 添加所有对象
	 * @param element
	 * @param <E>
	 */
	<E> void addAll(List<E> element);

}

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
package org.apache.ibatis.reflection.factory;

import java.util.List;
import java.util.Properties;

/**
 * MyBatis uses an ObjectFactory to create all needed new Objects.
 *
 * @author Clinton Begin
 *
 * 通过数据库查出来数据,如何转化成java对象,转化成java对象后如何创建对象
 * 就可以通过ObjectFactory工厂来进行了
 */
public interface ObjectFactory {

	/**
	 * Sets configuration properties.
	 * // 设置属性了,properties
	 * @param properties configuration properties
	 */
	void setProperties(Properties properties);

	/**
	 * Creates a new object with default constructor.
	 * 通过传入的Class 来创建对象了 使用默认构造器把
	 * @param type Object type
	 * @return
	 */
	<T> T create(Class<T> type);

	/**
	 * Creates a new object with the specified constructor and params.
	 * 通过传入的Class 来创建对象了 使用重载的构造器
	 * @param type Object type
	 * @param constructorArgTypes Constructor argument types
	 * @param constructorArgs Constructor argument values
	 * @return
	 */
	<T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

	/**
	 * Returns true if this object can have a set of other objects.
	 * It's main purpose is to support non-java.util.Collection objects like Scala collections.
	 * 判断这个对象是否关联集合
	 * @param type Object type
	 * @return whether it is a collection or not
	 * @since 3.1.0
	 */
	<T> boolean isCollection(Class<T> type);

}

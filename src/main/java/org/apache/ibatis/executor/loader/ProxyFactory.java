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
package org.apache.ibatis.executor.loader;

import java.util.List;
import java.util.Properties;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;

/**
 * @author Eduardo Macarron
 * 代理工厂又来了
 */
public interface ProxyFactory {

	/**
	 * set Properties
	 * @param properties
	 */
	void setProperties(Properties properties);

	/**
	 * 创建代理
	 * @param target 代理目标对象
	 * @param lazyLoader ResultLoaderMap
	 * @param configuration 配置信息
	 * @param objectFactory 对象工厂
	 * @param constructorArgTypes 构造参数值类型
	 * @param constructorArgs 构造参数值
	 * @return
	 */
	Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration, ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

}

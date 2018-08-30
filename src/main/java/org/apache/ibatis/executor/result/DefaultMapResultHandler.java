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
package org.apache.ibatis.executor.result;

import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

/**
 * @author Clinton Begin
 * 默认MapResult Handler 很简单这种就是泛型了
 * 针对 <map></map> 这种类似的标签
 *
 * 毕竟 由于数据库映射的特性 key value  value 都是一个类型的
 */
public class DefaultMapResultHandler<K, V> implements ResultHandler<V> {

	/**
	 * map 结果集
	 */
	private final Map<K, V> mappedResults;
	/**
	 * map 中的key
	 */
	private final String mapKey;
	/**
	 * 对象工厂 反射来了
	 */
	private final ObjectFactory objectFactory;
	private final ObjectWrapperFactory objectWrapperFactory;
	private final ReflectorFactory reflectorFactory;

	@SuppressWarnings("unchecked")
	public DefaultMapResultHandler(String mapKey, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
		this.objectFactory = objectFactory;
		this.objectWrapperFactory = objectWrapperFactory;
		this.reflectorFactory = reflectorFactory;
		this.mappedResults = objectFactory.create(Map.class);
		this.mapKey = mapKey;
	}

	@Override
	public void handleResult(ResultContext<? extends V> context) {
		// 核心方法
		final V value = context.getResultObject();
		// 直接获取value 的 对象元数据信息
		final MetaObject mo = MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
		// TODO is that assignment always true?
		final K key = (K) mo.getValue(mapKey);
		// 然后添加到封装的结果集中
		mappedResults.put(key, value);
	}

	public Map<K, V> getMappedResults() {
		return mappedResults;
	}
}

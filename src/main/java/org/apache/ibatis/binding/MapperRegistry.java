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

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 * Mybatis提倡面向接口编程,接口对应的方法就是我们的sql 我们需要注册,绑定
 * 接口需要存储,就用MapperRegistry来存储
 * 内部一个knownMappers
 * 然后就是get 和 set 方法对外提供接口
 *
 * 这里是所有的Mapper缓存的地方
 */
public class MapperRegistry {

	/**
	 * 配置类 里面应该是各种参数把
	 */
	private final Configuration config;
	/**
	 * 存储了各种 接口 以及接口对应的工厂
	 */
	private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<Class<?>, MapperProxyFactory<?>>();

	public MapperRegistry(Configuration config) {
		this.config = config;
	}

	@SuppressWarnings("unchecked")
	/**
	 * 直接通过 class 和 SqlSession 来获取Mapper实例
	 */
	public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
		// 直接通过map取出来
		final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
		if (mapperProxyFactory == null) {
			throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
		}
		try {
			// 直接通过工厂newInstance出来
			return mapperProxyFactory.newInstance(sqlSession);
		} catch (Exception e) {
			throw new BindingException("Error getting mapper instance. Cause: " + e, e);
		}
	}

	public <T> boolean hasMapper(Class<T> type) {
		return knownMappers.containsKey(type);
	}

	public <T> void addMapper(Class<T> type) {
		// 直接添加接口
		if (type.isInterface()) {
			// 判断是否已经绑定
			if (hasMapper(type)) {
				throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
			}
			// 标志位
			boolean loadCompleted = false;
			try {
				// 存储了
				knownMappers.put(type, new MapperProxyFactory<T>(type));
				// It's important that the type is added before the parser is run
				// otherwise the binding may automatically be attempted by the
				// mapper parser. If the type is already known, it won't try.
				// 这个操作很重要,直接通过config和传入的接口类进行注解映射,可以看的出来这里使用了建造者模式
				// 拿到接口 填充相关操作 然后反射方法 进行相关实现
				MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
				// 映射完毕后进行解析,只有解析成功的才可以放入到mapper中
				parser.parse();
				loadCompleted = true;
			} finally {
				if (!loadCompleted) {
					knownMappers.remove(type);
				}
			}
		}
	}

	/**
	 * @since 3.2.2
	 * 同样 不能修改的返回
	 */
	public Collection<Class<?>> getMappers() {
		return Collections.unmodifiableCollection(knownMappers.keySet());
	}

	/**
	 * @since 3.2.2
	 * 直接包名来进行操作
	 */
	public void addMappers(String packageName, Class<?> superType) {
		// 这个工具还记得把
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
		// 包下,使这个superType的类型都加在
		resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
		Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
		// 然后全部 add以下
		for (Class<?> mapperClass : mapperSet) {
			addMapper(mapperClass);
		}
	}

	/**
	 * @since 3.2.2
	 * 这个方法就封装上面方法
	 * 直接把packageName 目录下面的 Object的子类全部加在 可以认为是扫包把 哈哈
	 */
	public void addMappers(String packageName) {
		addMappers(packageName, Object.class);
	}

}

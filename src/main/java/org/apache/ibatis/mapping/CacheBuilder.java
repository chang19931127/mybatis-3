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
package org.apache.ibatis.mapping;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.builder.InitializingObject;
import org.apache.ibatis.cache.decorators.BlockingCache;
import org.apache.ibatis.cache.decorators.LoggingCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.ScheduledCache;
import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.decorators.SynchronizedCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * @author Clinton Begin
 * 缓存构建类
 *
 * 观察一下 属性都是用的对象类型 包装类
 */
public class CacheBuilder {
	/**
	 * id 什么id  缓存的id
	 */
	private final String id;
	/**
	 * 缓存的实现类
	 */
	private Class<? extends Cache> implementation;
	/**
	 * 各种缓存的实现类集合   缓存是装饰模式实现的
	 */
	private final List<Class<? extends Cache>> decorators;
	/**
	 * 缓存大小
	 */
	private Integer size;
	/**
	 * 清楚间隔
	 */
	private Long clearInterval;
	/**
	 * 是否读写
	 */
	private boolean readWrite;
	/**
	 * key value 对
	 */
	private Properties properties;
	/**
	 * 是否阻塞
	 */
	private boolean blocking;

	public CacheBuilder(String id) {
		this.id = id;
		this.decorators = new ArrayList<Class<? extends Cache>>();
	}

	public CacheBuilder implementation(Class<? extends Cache> implementation) {
		this.implementation = implementation;
		return this;
	}

	public CacheBuilder addDecorator(Class<? extends Cache> decorator) {
		if (decorator != null) {
			this.decorators.add(decorator);
		}
		return this;
	}

	public CacheBuilder size(Integer size) {
		this.size = size;
		return this;
	}

	public CacheBuilder clearInterval(Long clearInterval) {
		this.clearInterval = clearInterval;
		return this;
	}

	public CacheBuilder readWrite(boolean readWrite) {
		this.readWrite = readWrite;
		return this;
	}

	public CacheBuilder blocking(boolean blocking) {
		this.blocking = blocking;
		return this;
	}

	public CacheBuilder properties(Properties properties) {
		this.properties = properties;
		return this;
	}

	/**
	 * 重点是这个方法
	 * @return
	 */
	public Cache build() {
		setDefaultImplementations();
		Cache cache = newBaseCacheInstance(implementation, id);
		setCacheProperties(cache);
		// issue #352, do not apply decorators to custom caches
		if (PerpetualCache.class.equals(cache.getClass())) {
			// 装饰一遍
			for (Class<? extends Cache> decorator : decorators) {
				cache = newCacheDecoratorInstance(decorator, cache);
				setCacheProperties(cache);
			}
			// set标准内容 通过 参数吧
			cache = setStandardDecorators(cache);
		} else if (!LoggingCache.class.isAssignableFrom(cache.getClass())) {
			cache = new LoggingCache(cache);
		}
		return cache;
	}

	/**
	 * 设置默认的缓存实现      实在没有实现的基础上。
	 * 默认永恒缓存 lru 过期
	 */
	private void setDefaultImplementations() {
		if (implementation == null) {
			implementation = PerpetualCache.class;
			if (decorators.isEmpty()) {
				decorators.add(LruCache.class);
			}
		}
	}

	private Cache setStandardDecorators(Cache cache) {
		try {
			MetaObject metaCache = SystemMetaObject.forObject(cache);
			if (size != null && metaCache.hasSetter("size")) {
				metaCache.setValue("size", size);
			}
			if (clearInterval != null) {
				cache = new ScheduledCache(cache);
				((ScheduledCache) cache).setClearInterval(clearInterval);
			}
			if (readWrite) {
				cache = new SerializedCache(cache);
			}
			cache = new LoggingCache(cache);
			cache = new SynchronizedCache(cache);
			if (blocking) {
				cache = new BlockingCache(cache);
			}
			return cache;
		} catch (Exception e) {
			throw new CacheException("Error building standard cache decorators.  Cause: " + e, e);
		}
	}

	/**
	 * 给缓存 添加属性了啊 通过xml 装配到 Properties中 然后去 配置缓存.
	 * 其实这里的缓存是 本地缓存。
	 * @param cache
	 */
	private void setCacheProperties(Cache cache) {
		if (properties != null) {
			// 直接获取到一个对象的 元数据把  可以各种操作 而且是缓存性质的 性能较好
			MetaObject metaCache = SystemMetaObject.forObject(cache);
			for (Map.Entry<Object, Object> entry : properties.entrySet()) {
				String name = (String) entry.getKey();
				String value = (String) entry.getValue();
				if (metaCache.hasSetter(name)) {
					Class<?> type = metaCache.getSetterType(name);
					if (String.class == type) {
						metaCache.setValue(name, value);
					} else if (int.class == type
							|| Integer.class == type) {
						metaCache.setValue(name, Integer.valueOf(value));
					} else if (long.class == type
							|| Long.class == type) {
						metaCache.setValue(name, Long.valueOf(value));
					} else if (short.class == type
							|| Short.class == type) {
						metaCache.setValue(name, Short.valueOf(value));
					} else if (byte.class == type
							|| Byte.class == type) {
						metaCache.setValue(name, Byte.valueOf(value));
					} else if (float.class == type
							|| Float.class == type) {
						metaCache.setValue(name, Float.valueOf(value));
					} else if (boolean.class == type
							|| Boolean.class == type) {
						metaCache.setValue(name, Boolean.valueOf(value));
					} else if (double.class == type
							|| Double.class == type) {
						metaCache.setValue(name, Double.valueOf(value));
					} else {
						// 文档乱写属性就异常干掉你了
						throw new CacheException("Unsupported property type for cache: '" + name + "' of type " + type);
					}
				}
			}
		}
		if (InitializingObject.class.isAssignableFrom(cache.getClass())) {
			try {
				((InitializingObject) cache).initialize();
			} catch (Exception e) {
				throw new CacheException("Failed cache initialization for '" +
						cache.getId() + "' on '" + cache.getClass().getName() + "'", e);
			}
		}
	}

	/**
	 * 得到基础的缓存实例   其实就是通过反射,不过可以了解人家的 refactor method节奏感
	 * @param cacheClass
	 * @param id
	 * @return
	 */
	private Cache newBaseCacheInstance(Class<? extends Cache> cacheClass, String id) {
		Constructor<? extends Cache> cacheConstructor = getBaseCacheConstructor(cacheClass);
		try {
			return cacheConstructor.newInstance(id);
		} catch (Exception e) {
			throw new CacheException("Could not instantiate cache implementation (" + cacheClass + "). Cause: " + e, e);
		}
	}

	private Constructor<? extends Cache> getBaseCacheConstructor(Class<? extends Cache> cacheClass) {
		try {
			return cacheClass.getConstructor(String.class);
		} catch (Exception e) {
			throw new CacheException("Invalid base cache implementation (" + cacheClass + ").  " +
					"Base cache implementations must have a constructor that takes a String id as a parameter.  Cause: " + e, e);
		}
	}

	private Cache newCacheDecoratorInstance(Class<? extends Cache> cacheClass, Cache base) {
		Constructor<? extends Cache> cacheConstructor = getCacheDecoratorConstructor(cacheClass);
		try {
			return cacheConstructor.newInstance(base);
		} catch (Exception e) {
			throw new CacheException("Could not instantiate cache decorator (" + cacheClass + "). Cause: " + e, e);
		}
	}

	private Constructor<? extends Cache> getCacheDecoratorConstructor(Class<? extends Cache> cacheClass) {
		try {
			return cacheClass.getConstructor(Cache.class);
		} catch (Exception e) {
			throw new CacheException("Invalid cache decorator (" + cacheClass + ").  " +
					"Cache decorators must have a constructor that takes a Cache instance as a parameter.  Cause: " + e, e);
		}
	}
}

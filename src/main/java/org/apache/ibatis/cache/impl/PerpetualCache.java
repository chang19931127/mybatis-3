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
package org.apache.ibatis.cache.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * @author Clinton Begin
 * 没有读写锁
 * 永恒的缓存,字面意思    理解为什么永恒
 * 没有一点特殊功能  仅仅是认为控制存进去不会有过期时间
 *
 * 一个简单的java缓存 就是封装一个Map
 * 然后通过key 来获取value     然后自己去进行cache的控制 get put remove 均需要自己来控制
 *
 */
public class PerpetualCache implements Cache {

	/**
	 * 封装的CacheId值 用来表示Cache实例把
	 */
	private final String id;
	/**
	 * 封装的HashMap 用来实现缓存
	 */
	private Map<Object, Object> cache = new HashMap<Object, Object>();

	public PerpetualCache(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public int getSize() {
		return cache.size();
	}

	@Override
	public void putObject(Object key, Object value) {
		cache.put(key, value);
	}

	@Override
	public Object getObject(Object key) {
		return cache.get(key);
	}

	@Override
	public Object removeObject(Object key) {
		return cache.remove(key);
	}

	@Override
	public void clear() {
		cache.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (getId() == null) {
			throw new CacheException("Cache instances require an ID.");
		}
		if (this == o) {
			return true;
		}
		if (!(o instanceof Cache)) {
			return false;
		}

		Cache otherCache = (Cache) o;
		return getId().equals(otherCache.getId());
	}

	@Override
	public int hashCode() {
		if (getId() == null) {
			throw new CacheException("Cache instances require an ID.");
		}
		return getId().hashCode();
	}

}

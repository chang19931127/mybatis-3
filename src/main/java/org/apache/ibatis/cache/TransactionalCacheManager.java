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
package org.apache.ibatis.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.cache.decorators.TransactionalCache;

/**
 * @author Clinton Begin
 * 字面翻译过来,就是缓存事务管理器
 * 目的就是通过这个类来对缓存进行管理
 * 1. 管理器对Cache进行操作
 * 2. 通过TransactionalCache 进行相关的操作
 *
 * 其实就是封装了多个   Cache-TransactionalCache对
 * 这样可以对Cache进行操作  以及TransactionCache进行操作
 */
public class TransactionalCacheManager {

	/**
	 * 事务缓存 hashMap
	 * key 是Cache  value是 TransactionCache
	 */
	private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<Cache, TransactionalCache>();

	/**
	 * 通过缓存管理器清楚某个Cache的缓存
	 * @param cache
	 */
	public void clear(Cache cache) {
		getTransactionalCache(cache).clear();
	}

	/**
	 * 通过管理器 然后从Cache中的CacheKey 来获取到缓存的value对象
	 * @param cache
	 * @param key
	 * @return
	 */
	public Object getObject(Cache cache, CacheKey key) {
		return getTransactionalCache(cache).getObject(key);
	}

	/**
	 * 通过管理器来对Cache CacheKey 来韩村Value对象
	 * @param cache
	 * @param key
	 * @param value
	 */
	public void putObject(Cache cache, CacheKey key, Object value) {
		getTransactionalCache(cache).putObject(key, value);
	}

	/**
	 * 事物进行提交
	 * 针对封装的Map   拿到所有values TransactionCache对象然后集体 commit
	 */
	public void commit() {
		for (TransactionalCache txCache : transactionalCaches.values()) {
			txCache.commit();
		}
	}

	/**
	 * 集体进行回滚    所有values TransactionalCache.rollback
	 */
	public void rollback() {
		for (TransactionalCache txCache : transactionalCaches.values()) {
			txCache.rollback();
		}
	}

	/**
	 * 通过Cache对象 来获取他的CacheTransactional对象
	 * @param cache
	 * @return
	 */
	private TransactionalCache getTransactionalCache(Cache cache) {
		TransactionalCache txCache = transactionalCaches.get(cache);
		if (txCache == null) {
			txCache = new TransactionalCache(cache);
			transactionalCaches.put(cache, txCache);
		}
		return txCache;
	}

}

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
package org.apache.ibatis.cache.decorators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * The 2nd level cache transactional buffer.
 *
 * This class holds all cache entries that are to be added to the 2nd level cache during a Session.
 * Entries are sent to the cache when commit is called or discarded if the Session is rolled back. 
 * Blocking cache support has been added. Therefore any get() that returns a cache miss 
 * will be followed by a put() so any lock associated with the key can be released. 
 *
 * 二级事务缓存 这个类保持多个缓存 以及所有session期间的缓存
 * 内部封装一个Map用来存储所有缓存put的数据 等commit的时候put到真正的缓存中
 * 封装一个Set用来存储未命中的key,等rollback的时候全部remove
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class TransactionalCache implements Cache {

	private static final Log log = LogFactory.getLog(TransactionalCache.class);

	/**
	 * 缓存对象
	 */
	private final Cache delegate;
	/**
	 * 是否清楚所有commit的
	 */
	private boolean clearOnCommit;
	/**
	 * 所有put的 都放到这里
	 */
	private final Map<Object, Object> entriesToAddOnCommit;
	/**
	 * 存放所有没有命中的key
	 */
	private final Set<Object> entriesMissedInCache;

	public TransactionalCache(Cache delegate) {
		this.delegate = delegate;
		this.clearOnCommit = false;
		this.entriesToAddOnCommit = new HashMap<Object, Object>();
		this.entriesMissedInCache = new HashSet<Object>();
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public int getSize() {
		return delegate.getSize();
	}

	@Override
	public Object getObject(Object key) {
		// issue #116
		Object object = delegate.getObject(key);
		if (object == null) {
			// 如果缓存中没有key   就将这个key添加到entriesMissedInCache
			entriesMissedInCache.add(key);
		}
		// issue #146
		if (clearOnCommit) {
			return null;
		} else {
			return object;
		}
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	@Override
	public void putObject(Object key, Object object) {
		// 全体put缓存都放到 entriesToAddOnCommit
		entriesToAddOnCommit.put(key, object);
	}

	@Override
	public Object removeObject(Object key) {
		return null;
	}

	@Override
	public void clear() {
		clearOnCommit = true;
		entriesToAddOnCommit.clear();
	}

	/**
	 * 调用事物缓存的commit方法
	 */
	public void commit() {
		// 是否提交清除
		if (clearOnCommit) {
			// 缓存清除
			delegate.clear();
		}
		flushPendingEntries();
		reset();
	}

	/**
	 * rollback回滚了
	 */
	public void rollback() {
		unlockMissedEntries();
		reset();
	}

	private void reset() {
		clearOnCommit = false;
		entriesToAddOnCommit.clear();
		entriesMissedInCache.clear();
	}

	/**
	 * 刷新所有entry
	 */
	private void flushPendingEntries() {
		for (Map.Entry<Object, Object> entry : entriesToAddOnCommit.entrySet()) {
			// 将所有put在entriesToAddOnCommit 集合全部put到缓存中
			delegate.putObject(entry.getKey(), entry.getValue());
		}
		for (Object entry : entriesMissedInCache) {
			// 将所有未命中的全部null存入缓存
			if (!entriesToAddOnCommit.containsKey(entry)) {
				delegate.putObject(entry, null);
			}
		}
	}

	/**
	 * 事物回滚啊
	 */
	private void unlockMissedEntries() {
		for (Object entry : entriesMissedInCache) {
			// 将所有未命中的缓存全部remove
			try {
				delegate.removeObject(entry);
			} catch (Exception e) {
				log.warn("Unexpected exception while notifiying a rollback to the cache adapter."
						+ "Consider upgrading your cache adapter to the latest version.  Cause: " + e);
			}
		}
	}

}

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
package org.apache.ibatis.cache.decorators;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * Simple blocking decorator 
 *
 * Simple and inefficient version of EhCache's BlockingCache decorator.
 * It sets a lock over a cache key when the element is not found in cache.
 * This way, other threads will wait until this element is filled instead of hitting the database.
 *
 * @author Eduardo Macarron
 * 通过观看注释,了解到这是EhCache的简单并且低效版本
 * 如果访问的时候没有命中缓存会设置一个锁,知道数据被缓存,这种方式来阻止命中数据库
 * 直接给一个Cache提供锁操作,然后一个key一把ReentrantLock
 *
 * 阻塞Cache并没有真正的remove缓存,clean方法才会真正清空缓存
 */
public class BlockingCache implements Cache {

	/**
	 * 超时时间
	 */
	private long timeout;
	/**
	 * 装饰模式,装饰的Cache
	 */
	private final Cache delegate;
	/**
	 * 一个ConcurrentHashMap   来管理Object ReentrantLock  一个Object一把锁
	 * 一个key一把锁
	 */
	private final ConcurrentHashMap<Object, ReentrantLock> locks;

	public BlockingCache(Cache delegate) {
		this.delegate = delegate;
		this.locks = new ConcurrentHashMap<Object, ReentrantLock>();
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
	public void putObject(Object key, Object value) {
		try {
			// 直接通过装饰对象来put
			delegate.putObject(key, value);
		} finally {
			// 释放key 的锁
			releaseLock(key);
		}
	}

	@Override
	public Object getObject(Object key) {
		// 尝试获取key的锁,时间内没有获得就报异常了
		acquireLock(key);
		Object value = delegate.getObject(key);
		if (value != null) {
			releaseLock(key);
		}
		return value;
	}

	@Override
	public Object removeObject(Object key) {
		// despite of its name, this method is called only to release locks
		// 仅释放锁
		releaseLock(key);
		return null;
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		// 锁key的因此也不是所缓存的 就是null
		return null;
	}

	private ReentrantLock getLockForKey(Object key) {
		ReentrantLock lock = new ReentrantLock();
		// new 一把锁,如果这个对象有就直接返回,没有就添加并返回
		ReentrantLock previous = locks.putIfAbsent(key, lock);
		return previous == null ? lock : previous;
	}

	private void acquireLock(Object key) {
		Lock lock = getLockForKey(key);
		if (timeout > 0) {
			try {
				// 尝试获取锁多少秒
				boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
				if (!acquired) {
					throw new CacheException("Couldn't get a lock in " + timeout + " for the key " + key + " at the cache " + delegate.getId());
				}
			} catch (InterruptedException e) {
				throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
			}
		} else {
			lock.lock();
		}
	}

	/**
	 * 释放key的锁
	 * @param key
	 */
	private void releaseLock(Object key) {
		// 获得key 的锁
		ReentrantLock lock = locks.get(key);
		// 是否被当前线程保持
		if (lock.isHeldByCurrentThread()) {
			lock.unlock();
		}
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
}
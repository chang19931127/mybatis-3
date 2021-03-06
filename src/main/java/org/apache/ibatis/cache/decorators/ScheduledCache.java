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

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * @author Clinton Begin
 * 有计划的缓存,那么怎么一个有计划
 * 一定的时间间隔,就会清除一次缓存    Cache.clean
 * 只要针对缓存操作就会触发这个操作
 */
public class ScheduledCache implements Cache {

	/**
	 * 缓存对象
	 */
	private final Cache delegate;
	/**
	 * 清楚的间隔时间
	 */
	protected long clearInterval;
	/**
	 * 上次清楚的时间
	 */
	protected long lastClear;

	public ScheduledCache(Cache delegate) {
		this.delegate = delegate;
		// 默认就是1小时
		this.clearInterval = 60 * 60 * 1000; // 1 hour
		// 当前时间可以通过  System.currentTimeMillis() 这个数字是毫秒？
		this.lastClear = System.currentTimeMillis();
	}

	public void setClearInterval(long clearInterval) {
		this.clearInterval = clearInterval;
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public int getSize() {
		clearWhenStale();
		return delegate.getSize();
	}

	@Override
	public void putObject(Object key, Object object) {
		clearWhenStale();
		delegate.putObject(key, object);
	}

	@Override
	public Object getObject(Object key) {
		return clearWhenStale() ? null : delegate.getObject(key);
	}

	@Override
	public Object removeObject(Object key) {
		clearWhenStale();
		return delegate.removeObject(key);
	}

	@Override
	public void clear() {
		lastClear = System.currentTimeMillis();
		delegate.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	/**
	 * 触发清楚操作
	 * 如果时间间隔>clearInterval 就清楚一次缓存
	 * @return
	 */
	private boolean clearWhenStale() {
		if (System.currentTimeMillis() - lastClear > clearInterval) {
			// 调用clear方法
			clear();
			return true;
		}
		return false;
	}

}

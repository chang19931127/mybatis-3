/**
 * Copyright 2009-2017 the original author or authors.
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

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * @author Clinton Begin
 * Logging 这个应该就是缓存的时候提供日志支持了
 * 可以通过日志,让我们得知一些信息
 * 封装Log对象
 */
public class LoggingCache implements Cache {

	/**
	 * Log对象 mybatis中的日志
	 */
	private final Log log;
	/**
	 * 装饰缓存对象
	 */
	private final Cache delegate;
	/**
	 * 请求的次数
	 */
	protected int requests = 0;
	/**
	 * 命中的次数
	 */
	protected int hits = 0;

	public LoggingCache(Cache delegate) {
		this.delegate = delegate;
		this.log = LogFactory.getLog(getId());
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
	public void putObject(Object key, Object object) {
		delegate.putObject(key, object);
	}

	@Override
	public Object getObject(Object key) {
		// 每次get 的时候,都会进行打印,缓存的命中率
		requests++;
		final Object value = delegate.getObject(key);
		if (value != null) {
			hits++;
		}
		// 学习下debug 的日志怎么打
		if (log.isDebugEnabled()) {
			log.debug("Cache Hit Ratio [" + getId() + "]: " + getHitRatio());
		}
		return value;
	}

	@Override
	public Object removeObject(Object key) {
		return delegate.removeObject(key);
	}

	@Override
	public void clear() {
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

	private double getHitRatio() {
		// 计算缓存命中率了   通过    命中次数/请求总数
		return (double) hits / (double) requests;
	}

}

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
package org.apache.ibatis.reflection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 默认实现
 * 里面通过ConcurrentHashMap来缓存reflector对象
 * 也就是说java内部程序可以通过ConcurrentHashMap来进行程序级别的缓存
 * 反射性能不高,但是我们缓存了反射的对象,那么反射的性能还会是我们的瓶颈么.
 */
public class DefaultReflectorFactory implements ReflectorFactory {
	//可以设置是否开启缓存
	private boolean classCacheEnabled = true;
	private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<Class<?>, Reflector>();

	public DefaultReflectorFactory() {
	}

	@Override
	public boolean isClassCacheEnabled() {
		return classCacheEnabled;
	}

	@Override
	public void setClassCacheEnabled(boolean classCacheEnabled) {
		this.classCacheEnabled = classCacheEnabled;
	}

	@Override
	public Reflector findForClass(Class<?> type) {
		if (classCacheEnabled) {
			// synchronized (type) removed see issue #461
			Reflector cached = reflectorMap.get(type);
			if (cached == null) {
				cached = new Reflector(type);
				reflectorMap.put(type, cached);
			}
			return cached;
		} else {
			return new Reflector(type);
		}
	}

}

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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * Lru (least recently used) cache decorator
 * LRU 一样的,只是这回规则变了,是淘汰最近使用次数最少的
 * 也是针对key做文章
 * 缓存个数1024个
 *
 * 这里封装了LinkedHashMap 然后通过LinkedHashMap的特性remove掉key并获得这个对象
 * 然后在Cache中将key进行remove 重点就是removeEldestEntry
 * 所以说jdk 源码要多读啊
 * @author Clinton Begin
 */
public class LruCache implements Cache {
	/**
	 * 装饰对象
	 */
	private final Cache delegate;
	/**
	 * 通过keyMap 里面的key和value 都是key
	 */
	private Map<Object, Object> keyMap;
	/**
	 * 用来存放将要溢出的key
	 */
	private Object eldestKey;

	public LruCache(Cache delegate) {
		this.delegate = delegate;
		// 1024
		setSize(1024);
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public int getSize() {
		return delegate.getSize();
	}

	public void setSize(final int size) {
		// 通过LinkedHashMap 的removeEldestEntry 来实现自己的逻辑
		keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
			private static final long serialVersionUID = 4267176411845948333L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
				boolean tooBig = size() > size;
				if (tooBig) {
					// 如果太长了就remove eldest这个key 这个key就是头
					eldestKey = eldest.getKey();
				}
				return tooBig;
			}
		};
	}

	@Override
	public void putObject(Object key, Object value) {
		delegate.putObject(key, value);
		cycleKeyList(key);
	}

	@Override
	public Object getObject(Object key) {
		keyMap.get(key); //touch
		return delegate.getObject(key);
	}

	@Override
	public Object removeObject(Object key) {
		return delegate.removeObject(key);
	}

	@Override
	public void clear() {
		delegate.clear();
		keyMap.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	private void cycleKeyList(Object key) {
		// put key 并且remove eldestKey
		keyMap.put(key, key);
		if (eldestKey != null) {
			delegate.removeObject(eldestKey);
			eldestKey = null;
		}
	}

	public static void main(String[] args) {
		LinkedHashMap<Object, Object> map = new LinkedHashMap<Object, Object>(4, .75F, true) {
			private static final long serialVersionUID = 4267176411845948333L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
				boolean tooBig = size() > 3;
				if (tooBig) {
					// 如果太长了就remove eldest这个key 这个key就是头
					System.out.println(eldest.getKey().toString());
				}
				return tooBig;
			}
		};
		map.put("1","1");
		map.put("2","2");
		map.put("3","3");
		map.put("4","4");
		map.get("2");
		System.out.println(map);
		map.get("4");
		System.out.println(map);
		map.put("5","5");

		// 注意构造方法哈
		LinkedHashMap<String,String> map1 = new LinkedHashMap<>(4, .75F, true);
		map1.put("1","1");
		map1.put("2","2");
		map1.put("3","3");
		map1.put("4","4");
		System.out.println(map1);
		map1.get("2");
		System.out.println(map1);
	}
}

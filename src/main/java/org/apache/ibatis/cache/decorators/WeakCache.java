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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.impl.PerpetualCache;

/**
 * Weak Reference cache decorator.
 * Thanks to Dr. Heinz Kabutz for his guidance here.
 * 和SoftCache相同,只是这里变成了WeakCache
 * 弱引用需要gc
 * @author Clinton Begin
 */
public class WeakCache implements Cache {
	private final Deque<Object> hardLinksToAvoidGarbageCollection;
	private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;
	private final Cache delegate;
	private int numberOfHardLinks;

	public WeakCache(Cache delegate) {
		this.delegate = delegate;
		this.numberOfHardLinks = 256;
		this.hardLinksToAvoidGarbageCollection = new LinkedList<Object>();
		this.queueOfGarbageCollectedEntries = new ReferenceQueue<Object>();
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public int getSize() {
		removeGarbageCollectedItems();
		return delegate.getSize();
	}

	public void setSize(int size) {
		this.numberOfHardLinks = size;
	}

	@Override
	public void putObject(Object key, Object value) {
		removeGarbageCollectedItems();
		delegate.putObject(key, new WeakEntry(key, value, queueOfGarbageCollectedEntries));
	}

	@Override
	public Object getObject(Object key) {
		Object result = null;
		@SuppressWarnings("unchecked") // assumed delegate cache is totally managed by this cache
				WeakReference<Object> weakReference = (WeakReference<Object>) delegate.getObject(key);
		if (weakReference != null) {
			result = weakReference.get();
			if (result == null) {
				delegate.removeObject(key);
			} else {
				hardLinksToAvoidGarbageCollection.addFirst(result);
				if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
					hardLinksToAvoidGarbageCollection.removeLast();
				}
			}
		}
		return result;
	}

	@Override
	public Object removeObject(Object key) {
		removeGarbageCollectedItems();
		return delegate.removeObject(key);
	}

	@Override
	public void clear() {
		hardLinksToAvoidGarbageCollection.clear();
		removeGarbageCollectedItems();
		delegate.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	private void removeGarbageCollectedItems() {
		WeakEntry sv;
		while ((sv = (WeakEntry) queueOfGarbageCollectedEntries.poll()) != null) {
			delegate.removeObject(sv.key);
		}
	}

	private static class WeakEntry extends WeakReference<Object> {
		private final Object key;

		private WeakEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
			super(value, garbageCollectionQueue);
			this.key = key;
		}
	}

	public static void main(String[] args) {
		Cache cache = new PerpetualCache("110");
		cache = new WeakCache(cache);
		WeakReference sf = new WeakReference("1",new ReferenceQueue());
		cache.putObject("1",sf);
		cache.putObject("2","2");
		cache.putObject("3","3");
		System.out.println(cache.getSize());
		System.out.println(cache.getObject("3"));
		byte[] b1 = new byte[2*1024*1024];
		byte[] b2 = new byte[2*1024*1024];
		byte[] b3 = new byte[2*1024*1024];
		byte[] b4 = new byte[1*1024*1024];
//		System.gc();
//		Thread.sleep(5000);
		System.out.println(cache.getSize());
		System.out.println(cache.getObject("2"));
		System.out.println((cache.getObject("1")));
		cache.putObject("4","4");


		final int N = 3000000;
		cache = new SoftCache(new PerpetualCache("default"));
		for (int i = 0; i < N; i++) {
			byte[] array = new byte[5001]; //waste a bunch of memory
			array[5000] = 1;
			cache.putObject(i, array);
			Object value = cache.getObject(i);
			if (cache.getSize() < i + 1) {
				//System.out.println("Cache exceeded with " + (i + 1) + " entries.");
				break;
			}
		}
		System.out.println(cache.getSize());
	}

}

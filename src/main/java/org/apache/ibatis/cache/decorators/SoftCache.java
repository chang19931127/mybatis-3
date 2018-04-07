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
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.impl.PerpetualCache;

/**
 * Soft Reference cache decorator
 * Thanks to Dr. Heinz Kabutz for his guidance here.
 * Soft引用来整的缓存
 * 缓存的对象被SoftEntry对象封装是一个软引用对象SoftReference
 *
 * 命中的缓存放到强引用队列
 * 其他操作会触发引用队列去移除对象
 * 软引用不需要gc
 * 弱引用最终只要判断只有弱引用可到就会被回收
 * referenceQueue.poll回去收集的
 * @author Clinton Begin
 */
public class SoftCache implements Cache {

	/**
	 * 一个队列,用来强引用避免垃圾回收
	 */
	private final Deque<Object> hardLinksToAvoidGarbageCollection;
	/**
	 * 一个引用队列,用来引用,垃圾回收的时候会进行处理
	 * 这里面存储一些软引用对象
	 * 通过引用队列,存储仅只有软引用的对象
	 */
	private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;
	/**
	 * 缓存装饰对象
	 */
	private final Cache delegate;
	/**
	 * 强引用关系个数
	 */
	private int numberOfHardLinks;

	public SoftCache(Cache delegate) {
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
		// put的对象被SoftEntry封装
		delegate.putObject(key, new SoftEntry(key, value, queueOfGarbageCollectedEntries));
	}

	@Override
	public Object getObject(Object key) {
		Object result = null;
		@SuppressWarnings("unchecked") // assumed delegate cache is totally managed by this cache
				// SoftEntry对象
				SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
		if (softReference != null) {
			// 通过get方法获取以下
			result = softReference.get();
			if (result == null) {
				// null 就直接 缓存remove
				delegate.removeObject(key);
			} else {
				// See #586 (and #335) modifications need more than a read lock
				synchronized (hardLinksToAvoidGarbageCollection) {
					// 缓存被用到过,放到队列中形成强一弄关系
					hardLinksToAvoidGarbageCollection.addFirst(result);
					if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
						// 如果缓存数量多了,就将最早的移除
						hardLinksToAvoidGarbageCollection.removeLast();
					}
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
		synchronized (hardLinksToAvoidGarbageCollection) {
			hardLinksToAvoidGarbageCollection.clear();
		}
		removeGarbageCollectedItems();
		delegate.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	/**
	 * 通过软引用的引用连关系,来进行一波引用对象的remove
	 */
	private void removeGarbageCollectedItems() {
		SoftEntry sv;
		// while 遍历所有软引用   poll轮训软引用队列是否被其他引用
		while ((sv = (SoftEntry) queueOfGarbageCollectedEntries.poll()) != null) {
			// remove
			delegate.removeObject(sv.key);
		}
	}

	private static class SoftEntry extends SoftReference<Object> {
		private final Object key;

		SoftEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
			// key ,value ,引用队列
			super(value, garbageCollectionQueue);
			this.key = key;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		Cache cache = new PerpetualCache("110");
		cache = new SoftCache(cache);
		SoftReference sf = new SoftReference("1",new ReferenceQueue());
		cache.putObject("1",sf);
		cache.putObject("2","2");
		cache.putObject("3","3");
		cache.putObject("4","4");
		cache.putObject("5","5");
		cache.putObject("6","6");
		cache.putObject("7","7");
		cache.putObject("8","8");
		cache.putObject("9","9");
		System.out.println(cache.getSize());
		System.out.println(cache.getObject("3"));
		byte[] b1 = new byte[2*1024*1024];
		byte[] b2 = new byte[2*1024*1024];
		byte[] b3 = new byte[2*1024*1024];
		byte[] b4 = new byte[1*1024*1024];
		int i = 0;
		while(true){
			cache.putObject(i,i);
			i++;
			System.gc();
			System.out.println(cache.getSize());
			System.out.println(cache.getObject("2"));
			System.out.println((cache.getObject("1")));
		}
//		final int N = 3000000;
//		cache = new SoftCache(new PerpetualCache("default"));
//		for (int i = 0; i < N; i++) {
//			byte[] array = new byte[5001]; //waste a bunch of memory
//			array[5000] = 1;
//			cache.putObject(i, array);
//			// 注释掉就全部被ReferenceQueue清除了
////			Object value = cache.getObject(i);
//			if (cache.getSize() < i + 1) {
//				//System.out.println("Cache exceeded with " + (i + 1) + " entries.");
//				break;
//			}
//		}
//		// 不确定因素太大,所以一次GC很难模拟出来
//		System.out.println(cache.getSize());
	}

}
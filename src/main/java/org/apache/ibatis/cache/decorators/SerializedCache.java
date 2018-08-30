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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.io.Resources;

/**
 * @author Clinton Begin
 * 让缓存拥有序列化的能力,主要就是两个方法
 * serialize
 * deserialize 两个方法来序列化和反序列化
 *
 * 这个缓存存入的value 是序列化的内容,目的是什么可能为了减少空间把
 */
public class SerializedCache implements Cache {

	/**
	 * 装饰的缓存对象
	 */
	private final Cache delegate;

	public SerializedCache(Cache delegate) {
		this.delegate = delegate;
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
		if (object == null || object instanceof Serializable) {
			// 序列化后存入到value中
			delegate.putObject(key, serialize((Serializable) object));
		} else {
			throw new CacheException("SharedCache failed to make a copy of a non-serializable object: " + object);
		}
	}

	@Override
	public Object getObject(Object key) {
		Object object = delegate.getObject(key);
		return object == null ? null : deserialize((byte[]) object);
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

	/**
	 * 就仅仅是序列化的方法了,通过ObjectOutputStream来进行序列化
	 * @param value
	 * @return
	 */
	private byte[] serialize(Serializable value) {
		try {
			// 内存输出流
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			// 对象输出流 装饰模式
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			// 进行输出
			oos.writeObject(value);
			oos.flush();
			oos.close();
			return bos.toByteArray();
		} catch (Exception e) {
			throw new CacheException("Error serializing object.  Cause: " + e, e);
		}
	}

	private Serializable deserialize(byte[] value) {
		// 将对象从流中读取出来
		Serializable result;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(value);
			ObjectInputStream ois = new CustomObjectInputStream(bis);
			result = (Serializable) ois.readObject();
			ois.close();
		} catch (Exception e) {
			throw new CacheException("Error deserializing object.  Cause: " + e, e);
		}
		return result;
	}

	public static class CustomObjectInputStream extends ObjectInputStream {

		public CustomObjectInputStream(InputStream in) throws IOException {
			super(in);
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
			// 直接资源读取Class对象
			return Resources.classForName(desc.getName());
		}

	}

}

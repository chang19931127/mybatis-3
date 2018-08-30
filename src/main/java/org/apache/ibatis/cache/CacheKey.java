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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.reflection.ArrayUtil;

/**
 * @author Clinton Begin
 * mybatis内部使用cache的时候通过Cache来封装自己的key把
 * 实现接口Cloneable,Serializable接口
 * 就是封装一个ArrayList 如参是Object 或者Object数组
 * 具有一些功能,可以clone可以序列化,可以有hashCode,有一个校验和
 * 就是作为缓存的key来使用,让key更加复杂哈哈
 *
 *
 */
public class CacheKey implements Cloneable, Serializable {

	private static final long serialVersionUID = 1146682552656046210L;

	/**
	 * 提供一个NullCacheKay 可以直接被别人调用,就是里面什么都没有
	 */
	public static final CacheKey NULL_CACHE_KEY = new NullCacheKey();

	/**
	 * 默认的乘数multiplier 去理解 37
	 */
	private static final int DEFAULT_MULTIPLYER = 37;
	/**
	 * 默认的hashCode操作 去理解 17
	 */
	private static final int DEFAULT_HASHCODE = 17;

	private final int multiplier;
	private int hashcode;
	/**
	 * 校验和,都是hashCode累加的
	 */
	private long checksum;
	/**
	 * 记录update的个数
	 */
	private int count;
	// 8/21/2017 - Sonarlint flags this as needing to be marked transient.  While true if content is not serializable, this is not always true and thus should not be marked transient.
	/**
	 * 一个List 存储更新的对象把   放到transient
	 */
	private List<Object> updateList;

	public CacheKey() {
		// 默认构造参数构造四个默认属性
		this.hashcode = DEFAULT_HASHCODE;
		this.multiplier = DEFAULT_MULTIPLYER;
		this.count = 0;
		this.updateList = new ArrayList<Object>();
	}

	public CacheKey(Object[] objects) {
		this();
		updateAll(objects);
	}

	/**
	 * 获得更新的个数
	 * @return
	 */
	public int getUpdateCount() {
		return updateList.size();
	}

	/**
	 * 更新对象
	 * @param object
	 */
	public void update(Object object) {
		//拿到hash值
		int baseHashCode = object == null ? 1 : ArrayUtil.hashCode(object);
		//只要调用这个方法就count++
		count++;
		checksum += baseHashCode;
		baseHashCode *= count;

		// hashCode = 37 * 17 + baseHashCode   两个都是质数不容易冲突把
		// hashCode = 100101 * 10001
		hashcode = multiplier * hashcode + baseHashCode;

		// 完之后 add到updateList
		updateList.add(object);
	}

	/**
	 * 更新对象数组,其实 foreach Object
	 * @param objects
	 */
	public void updateAll(Object[] objects) {
		for (Object o : objects) {
			update(o);
		}
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof CacheKey)) {
			return false;
		}

		final CacheKey cacheKey = (CacheKey) object;

		if (hashcode != cacheKey.hashcode) {
			return false;
		}
		if (checksum != cacheKey.checksum) {
			return false;
		}
		if (count != cacheKey.count) {
			return false;
		}

		for (int i = 0; i < updateList.size(); i++) {
			Object thisObject = updateList.get(i);
			Object thatObject = cacheKey.updateList.get(i);
			if (!ArrayUtil.equals(thisObject, thatObject)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return hashcode;
	}

	@Override
	public String toString() {
		StringBuilder returnValue = new StringBuilder().append(hashcode).append(':').append(checksum);
		for (Object object : updateList) {
			returnValue.append(':').append(ArrayUtil.toString(object));
		}
		return returnValue.toString();
	}

	@Override
	public CacheKey clone() throws CloneNotSupportedException {
		CacheKey clonedCacheKey = (CacheKey) super.clone();
		clonedCacheKey.updateList = new ArrayList<Object>(updateList);
		return clonedCacheKey;
	}

	public static void main(String[] args) {
		CacheKey cacheKey = new CacheKey();
		Object[] objects = new Object[]{"123",17,true};
		cacheKey.updateAll(objects);
		System.out.println(cacheKey);
	}

}

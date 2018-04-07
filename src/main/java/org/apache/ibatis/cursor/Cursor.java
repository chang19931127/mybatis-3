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
package org.apache.ibatis.cursor;

import java.io.Closeable;

/**
 * Cursor contract to handle fetching items lazily using an Iterator.
 * Cursors are a perfect fit to handle millions of items queries that would not normally fits in memory.
 * Cursor SQL queries must be ordered (resultOrdered="true") using the id columns of the resultMap.
 *
 * @author Guillaume Darmont / guillaume@dropinocean.com
 * 游标就像使用迭代器一样拉去数据
 * 游标适合查询百万的数据但是不全部加载到内存的
 *  游标sql 查询必须使用resultMap 的id resultOrdered="true"进行排序
 *  资源接口Closeable
 *  游标类似迭代器Iterable操作
 */
public interface Cursor<T> extends Closeable, Iterable<T> {

	/**
	 * @return true if the cursor has started to fetch items from database.
	 * 是否开启游标 从数据库获取项
	 */
	boolean isOpen();

	/**
	 * @return true if the cursor is fully consumed and has returned all elements matching the query.
	 * 是否游标走完了,所有结果都已经消费
	 *
	 */
	boolean isConsumed();

	/**
	 * Get the current item index. The first item has the index 0.
	 * 获取当前游标 index位置
	 *
	 * @return -1 if the first cursor item has not been retrieved. The index of the current item retrieved.
	 */
	int getCurrentIndex();
}

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
package org.apache.ibatis.session;

/**
 * @author Clinton Begin
 * 行数绑定了额
 * 主要这个对象 set后 不能修改啊
 *
 * 也就是我们常说的 start limit
 * 也就是mysql总   limit 100,10
 *
 * 但是MyBatis的 分页是逻辑分页 例如 100,10   起始取出了110条数据 只给你了最后10条
 *
 */
public class RowBounds {

	/**
	 * 数据库起始行
	 */
	public static final int NO_ROW_OFFSET = 0;

	/**
	 * 然后拿到的数据
	 */
	public static final int NO_ROW_LIMIT = Integer.MAX_VALUE;
	public static final RowBounds DEFAULT = new RowBounds();

	private final int offset;
	private final int limit;

	public RowBounds() {
		this.offset = NO_ROW_OFFSET;
		this.limit = NO_ROW_LIMIT;
	}

	public RowBounds(int offset, int limit) {
		this.offset = offset;
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}

	public int getLimit() {
		return limit;
	}

}

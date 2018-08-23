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
package org.apache.ibatis.executor;

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * @author Clinton Begin
 * Executor 的 接口定义
 *
 * 执行器 都有哪些方法
 * 增删改查 事物回滚 缓存获取 等等
 */
public interface Executor {

	/**
	 * 常量 没有结果集处理类
	 */
	ResultHandler NO_RESULT_HANDLER = null;

	/**
	 * 更新方法 statement 和 参数
	 * @param ms
	 * @param parameter
	 * @return
	 * @throws SQLException
	 */
	int update(MappedStatement ms, Object parameter) throws SQLException;

	/**
	 * 泛型 查询 根据条件
	 * @param ms
	 * @param parameter
	 * @param rowBounds
	 * @param resultHandler
	 * @param cacheKey
	 * @param boundSql
	 * @param <E>
	 * @return
	 * @throws SQLException
	 */
	<E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException;

	<E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException;


	/**
	 * 通过 cursor 查询
	 * @param ms
	 * @param parameter
	 * @param rowBounds
	 * @param <E>
	 * @return
	 * @throws SQLException
	 */
	<E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException;

	/**
	 * 刷新一遍 statement
	 * @return
	 * @throws SQLException
	 */
	List<BatchResult> flushStatements() throws SQLException;

	/**
	 * 提交 commit
	 * @param required
	 * @throws SQLException
	 */
	void commit(boolean required) throws SQLException;

	/**
	 * 回滚
	 * @param required
	 * @throws SQLException
	 */
	void rollback(boolean required) throws SQLException;

	/**
	 * 创建缓存key
	 * @param ms
	 * @param parameterObject
	 * @param rowBounds
	 * @param boundSql
	 * @return
	 */
	CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql);

	/**
	 * 是否有缓存
	 * @param ms
	 * @param key
	 * @return
	 */
	boolean isCached(MappedStatement ms, CacheKey key);

	/**
	 * 清楚本地缓存
	 */
	void clearLocalCache();

	/**
	 * 默认加载
	 * @param ms
	 * @param resultObject
	 * @param property
	 * @param key
	 * @param targetType
	 */
	void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType);

	/**
	 * 获取事务
	 * @return
	 */
	Transaction getTransaction();

	/**
	 * 关闭吧
	 * @param forceRollback
	 */
	void close(boolean forceRollback);

	/**
	 * 是否关闭
	 * @return
	 */
	boolean isClosed();

	/**
	 * Executor 也可以有 包装 装饰模式
	 * @param executor
	 */
	void setExecutorWrapper(Executor executor);

}

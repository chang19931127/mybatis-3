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
package org.apache.ibatis.executor.loader;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.ResultExtractor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

/**
 * @author Clinton Begin
 * 结果 loader
 */
public class ResultLoader {

	// 内部的相关内容 配置信息 执行 语句 参数对象 对象工厂 cacheKey BoundSql预期 结果会执行 创建的id

	protected final Configuration configuration;
	protected final Executor executor;
	protected final MappedStatement mappedStatement;
	protected final Object parameterObject;
	protected final Class<?> targetType;
	protected final ObjectFactory objectFactory;
	protected final CacheKey cacheKey;
	protected final BoundSql boundSql;
	/**
	 * 结果抽出
	 */
	protected final ResultExtractor resultExtractor;
	protected final long creatorThreadId;

	protected boolean loaded;
	protected Object resultObject;

	public ResultLoader(Configuration config, Executor executor, MappedStatement mappedStatement, Object parameterObject, Class<?> targetType, CacheKey cacheKey, BoundSql boundSql) {
		this.configuration = config;
		this.executor = executor;
		this.mappedStatement = mappedStatement;
		this.parameterObject = parameterObject;
		this.targetType = targetType;
		this.objectFactory = configuration.getObjectFactory();
		this.cacheKey = cacheKey;
		this.boundSql = boundSql;
		this.resultExtractor = new ResultExtractor(configuration, objectFactory);
		this.creatorThreadId = Thread.currentThread().getId();
	}

	/**
	 * 加载结果
	 * @return
	 * @throws SQLException
	 */
	public Object loadResult() throws SQLException {
		List<Object> list = selectList();
		resultObject = resultExtractor.extractObjectFromList(list, targetType);
		return resultObject;
	}

	/**
	 * 选择list
	 * @param <E>
	 * @return
	 * @throws SQLException
	 */
	private <E> List<E> selectList() throws SQLException {
		Executor localExecutor = executor;
		// executor 如果关闭了 就创建一个
		if (Thread.currentThread().getId() != this.creatorThreadId || localExecutor.isClosed()) {
			localExecutor = newExecutor();
		}
		try {
			// 然后通过相关的条件 进行 query 查询
			return localExecutor.<E>query(mappedStatement, parameterObject, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER, cacheKey, boundSql);
		} finally {
			// Executor 使用完毕 需要关闭
			if (localExecutor != executor) {
				localExecutor.close(false);
			}
		}
	}

	/**
	 * 创建一个 Executor 简单的
	 * @return
	 */
	private Executor newExecutor() {
		final Environment environment = configuration.getEnvironment();
		if (environment == null) {
			throw new ExecutorException("ResultLoader could not load lazily.  Environment was not configured.");
		}
		final DataSource ds = environment.getDataSource();
		if (ds == null) {
			throw new ExecutorException("ResultLoader could not load lazily.  DataSource was not configured.");
		}
		final TransactionFactory transactionFactory = environment.getTransactionFactory();
		final Transaction tx = transactionFactory.newTransaction(ds, null, false);

		// 创建一个简单的Executor 事物
		return configuration.newExecutor(tx, ExecutorType.SIMPLE);
	}

	public boolean wasNull() {
		return resultObject == null;
	}

}

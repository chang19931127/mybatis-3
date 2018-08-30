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
package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 * statementHandler 的 base 处理了,内部有很多的变量可以使用了
 *
 * 这个类,我们可以效仿BaseController
 */
public abstract class BaseStatementHandler implements StatementHandler {

	// 五件套 配置 对象 类型处理 结果集 参数 处理

	protected final Configuration configuration;
	protected final ObjectFactory objectFactory;
	protected final TypeHandlerRegistry typeHandlerRegistry;
	protected final ResultSetHandler resultSetHandler;
	protected final ParameterHandler parameterHandler;

	// 三件套 执行器 映射语句 结果集

	protected final Executor executor;
	protected final MappedStatement mappedStatement;
	protected final RowBounds rowBounds;

	// 一个真实 BoundSql

	protected BoundSql boundSql;

	protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
		this.configuration = mappedStatement.getConfiguration();
		this.executor = executor;
		this.mappedStatement = mappedStatement;
		this.rowBounds = rowBounds;

		this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
		this.objectFactory = configuration.getObjectFactory();

		if (boundSql == null) {
			// issue #435, get the key before calculating the statement
			// 在调用 sql 之前,进行key生成
			generateKeys(parameterObject);
			boundSql = mappedStatement.getBoundSql(parameterObject);
		}

		this.boundSql = boundSql;

		this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
		this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
	}

	@Override
	public BoundSql getBoundSql() {
		return boundSql;
	}

	@Override
	public ParameterHandler getParameterHandler() {
		return parameterHandler;
	}

	@Override
	public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
		ErrorContext.instance().sql(boundSql.getSql());
		Statement statement = null;
		try {
			statement = instantiateStatement(connection);
			setStatementTimeout(statement, transactionTimeout);
			setFetchSize(statement);
			return statement;
		} catch (SQLException e) {
			closeStatement(statement);
			throw e;
		} catch (Exception e) {
			closeStatement(statement);
			throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
		}
	}

	/**
	 * 模板方法 通过连接来 获得Statement
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	protected abstract Statement instantiateStatement(Connection connection) throws SQLException;

	/**
	 * 给语句设置相应的超时 时间 这里使用到了 StatementUtil这个工具类
	 * @param stmt
	 * @param transactionTimeout
	 * @throws SQLException
	 */
	protected void setStatementTimeout(Statement stmt, Integer transactionTimeout) throws SQLException {
		Integer queryTimeout = null;
		if (mappedStatement.getTimeout() != null) {
			queryTimeout = mappedStatement.getTimeout();
		} else if (configuration.getDefaultStatementTimeout() != null) {
			queryTimeout = configuration.getDefaultStatementTimeout();
		}
		if (queryTimeout != null) {
			stmt.setQueryTimeout(queryTimeout);
		}
		StatementUtil.applyTransactionTimeout(stmt, queryTimeout, transactionTimeout);
	}

	/**
	 * 给语句设置 fetchSize 获取的个数
	 * @param stmt
	 * @throws SQLException
	 */
	protected void setFetchSize(Statement stmt) throws SQLException {
		Integer fetchSize = mappedStatement.getFetchSize();
		if (fetchSize != null) {
			stmt.setFetchSize(fetchSize);
			return;
		}
		Integer defaultFetchSize = configuration.getDefaultFetchSize();
		if (defaultFetchSize != null) {
			stmt.setFetchSize(defaultFetchSize);
		}
	}

	/**
	 * 关闭Statement连接
	 * @param statement
	 */
	protected void closeStatement(Statement statement) {
		try {
			if (statement != null) {
				statement.close();
			}
		} catch (SQLException e) {
			//ignore
		}
	}

	/**
	 * 直接给语句去生成key
	 * 在执行语句之前 生成key
	 * @param parameter
	 */
	protected void generateKeys(Object parameter) {
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		ErrorContext.instance().store();
		keyGenerator.processBefore(executor, mappedStatement, null, parameter);
		ErrorContext.instance().recall();
	}

}

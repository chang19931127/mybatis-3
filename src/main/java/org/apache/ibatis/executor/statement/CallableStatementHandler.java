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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.JdbcType;

/**
 * @author Clinton Begin
 * CallableStatement 的 Handler
 * 就是存储过程的Statement
 * 这里就需要实现相应的方法
 * 每个方法都转型成对应的操作
 *
 * 执行完毕主要是这个方法resultSetHandler.handleOutputParameters(cs);
 */
public class CallableStatementHandler extends BaseStatementHandler {

	public CallableStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
		super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
	}

	@Override
	public int update(Statement statement) throws SQLException {
		CallableStatement cs = (CallableStatement) statement;
		// 执行存储过程
		cs.execute();
		// 或得到结果集
		int rows = cs.getUpdateCount();
		Object parameterObject = boundSql.getParameterObject();
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		// 执行之后来填充key
		keyGenerator.processAfter(executor, mappedStatement, cs, parameterObject);
		// 使用ResultSetHandler 来处理输出结果
		resultSetHandler.handleOutputParameters(cs);
		return rows;
	}

	@Override
	public void batch(Statement statement) throws SQLException {
		CallableStatement cs = (CallableStatement) statement;
		// 就是添加
		cs.addBatch();
	}

	@Override
	public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
		CallableStatement cs = (CallableStatement) statement;
		cs.execute();
		// 查询完毕后 进行 ResultSetHandler
		List<E> resultList = resultSetHandler.<E>handleResultSets(cs);
		resultSetHandler.handleOutputParameters(cs);
		return resultList;
	}

	@Override
	public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
		CallableStatement cs = (CallableStatement) statement;
		cs.execute();
		// 这个泛型方法可以了解一下
		Cursor<E> resultList = resultSetHandler.<E>handleCursorResultSets(cs);
		resultSetHandler.handleOutputParameters(cs);
		return resultList;
	}

	@Override
	protected Statement instantiateStatement(Connection connection) throws SQLException {
		// 通过boundSql 来操作一波然后配合connection 根据 sql 获得Statement
		// 根据不同的类 返回不同的 statement
		String sql = boundSql.getSql();
		if (mappedStatement.getResultSetType() != null) {
			return connection.prepareCall(sql, mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
		} else {
			return connection.prepareCall(sql);
		}
	}

	@Override
	public void parameterize(Statement statement) throws SQLException {

		registerOutputParameters((CallableStatement) statement);
		// 直接ParameterHandler 来给statement 进行参数操作
		parameterHandler.setParameters((CallableStatement) statement);
	}

	/**
	 * 输出参数 进行拼装赞书画
	 * @param cs
	 * @throws SQLException
	 */
	private void registerOutputParameters(CallableStatement cs) throws SQLException {
		// 获得参数映射
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		for (int i = 0, n = parameterMappings.size(); i < n; i++) {
			// 循环参数映射 然后进行拼装 进行注册
			ParameterMapping parameterMapping = parameterMappings.get(i);
			if (parameterMapping.getMode() == ParameterMode.OUT || parameterMapping.getMode() == ParameterMode.INOUT) {
				if (null == parameterMapping.getJdbcType()) {
					throw new ExecutorException("The JDBC Type must be specified for output parameter.  Parameter: " + parameterMapping.getProperty());
				} else {
					if (parameterMapping.getNumericScale() != null && (parameterMapping.getJdbcType() == JdbcType.NUMERIC || parameterMapping.getJdbcType() == JdbcType.DECIMAL)) {
						cs.registerOutParameter(i + 1, parameterMapping.getJdbcType().TYPE_CODE, parameterMapping.getNumericScale());
					} else {
						if (parameterMapping.getJdbcTypeName() == null) {
							cs.registerOutParameter(i + 1, parameterMapping.getJdbcType().TYPE_CODE);
						} else {
							cs.registerOutParameter(i + 1, parameterMapping.getJdbcType().TYPE_CODE, parameterMapping.getJdbcTypeName());
						}
					}
				}
			}
		}
	}

}

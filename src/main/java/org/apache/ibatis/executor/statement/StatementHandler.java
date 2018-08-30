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
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.ResultHandler;

/**
 * @author Clinton Begin
 * 语句处理
 * 针对语句 需要准备 然后各种处理
 * 这里就是接口相关操作
 * 要明白StatementHandler 内部肯定封装了Statement 相关的内容
 */
public interface StatementHandler {

	/**
	 * 直接获取 Statement
	 * @param connection
	 * @param transactionTimeout
	 * @return
	 * @throws SQLException
	 */
	Statement prepare(Connection connection, Integer transactionTimeout)
			throws SQLException;

	/**
	 * 参数化
	 * @param statement
	 * @throws SQLException
	 */
	void parameterize(Statement statement)
			throws SQLException;

	/**
	 * 批处理华
	 * @param statement
	 * @throws SQLException
	 */
	void batch(Statement statement)
			throws SQLException;

	/**
	 * 更新语句
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	int update(Statement statement)
			throws SQLException;

	/**
	 * 查询语句
	 * @param statement
	 * @param resultHandler
	 * @param <E>
	 * @return
	 * @throws SQLException
	 */
	<E> List<E> query(Statement statement, ResultHandler resultHandler)
			throws SQLException;

	/**
	 * 带有表的查询
	 * @param statement
	 * @param <E>
	 * @return
	 * @throws SQLException
	 */
	<E> Cursor<E> queryCursor(Statement statement)
			throws SQLException;

	/**
	 * 获得 boudSql
	 * @return
	 */
	BoundSql getBoundSql();

	/**
	 * 获取 参数处理类
	 * @return
	 */
	ParameterHandler getParameterHandler();

}

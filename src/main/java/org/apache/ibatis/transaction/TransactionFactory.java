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
package org.apache.ibatis.transaction;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;

/**
 * Creates {@link Transaction} instances.
 * 事物管理器
 * @author Clinton Begin
 */
public interface TransactionFactory {

	/**
	 * Sets transaction factory custom properties.
	 * 少不了的配置文件来设置一些参数
	 * @param props
	 */
	void setProperties(Properties props);

	/**
	 * Creates a {@link Transaction} out of an existing connection.
	 * 通过Connection 来获得Transaction对象
	 * @param conn Existing database connection
	 * @return Transaction
	 * @since 3.1.0
	 */
	Transaction newTransaction(Connection conn);

	/**
	 * Creates a {@link Transaction} out of a datasource.
	 * 通过DataSource 配合数据库连接池来获得Transaction
	 * @param dataSource DataSource to take the connection from
	 * @param level Desired isolation level
	 * @param autoCommit Desired autocommit
	 * @return Transaction
	 * @since 3.1.0
	 */
	Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit);

}

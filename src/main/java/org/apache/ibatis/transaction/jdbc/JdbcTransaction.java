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
package org.apache.ibatis.transaction.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionException;

/**
 * {@link Transaction} that makes use of the JDBC commit and rollback facilities directly.
 * It relies on the connection retrieved from the dataSource to manage the scope of the transaction.
 * Delays connection retrieval until getConnection() is called.
 * Ignores commit or rollback requests when autocommit is on.
 *
 * jdbc事务管理器
 * @author Clinton Begin
 *
 * @see JdbcTransactionFactory
 */
public class JdbcTransaction implements Transaction {

	private static final Log log = LogFactory.getLog(JdbcTransaction.class);

	/**
	 * 连接对象
	 */
	protected Connection connection;
	/**
	 * 连接池对象
	 */
	protected DataSource dataSource;
	/**
	 * 事务个理解别
	 */
	protected TransactionIsolationLevel level;
	/**
	 * 是否泽东提交
	 */
	protected boolean autoCommit;

	public JdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
		dataSource = ds;
		level = desiredLevel;
		autoCommit = desiredAutoCommit;
	}

	public JdbcTransaction(Connection connection) {
		this.connection = connection;
	}

	@Override
	public Connection getConnection() throws SQLException {
		if (connection == null) {
			openConnection();
		}
		return connection;
	}

	@Override
	public void commit() throws SQLException {
		if (connection != null && !connection.getAutoCommit()) {
			if (log.isDebugEnabled()) {
				log.debug("Committing JDBC Connection [" + connection + "]");
			}
			// 直接connection commit
			connection.commit();
		}
	}

	@Override
	public void rollback() throws SQLException {
		if (connection != null && !connection.getAutoCommit()) {
			if (log.isDebugEnabled()) {
				log.debug("Rolling back JDBC Connection [" + connection + "]");
			}
			// 直接rollback
			connection.rollback();
		}
	}

	@Override
	public void close() throws SQLException {
		if (connection != null) {
			// 关闭事务
			resetAutoCommit();
			if (log.isDebugEnabled()) {
				log.debug("Closing JDBC Connection [" + connection + "]");
			}
			// 并且关闭连接
			connection.close();
		}
	}

	/**
	 * 设置connection的commit
	 * @param desiredAutoCommit
	 */
	protected void setDesiredAutoCommit(boolean desiredAutoCommit) {
		try {
			if (connection.getAutoCommit() != desiredAutoCommit) {
				if (log.isDebugEnabled()) {
					log.debug("Setting autocommit to " + desiredAutoCommit + " on JDBC Connection [" + connection + "]");
				}
				connection.setAutoCommit(desiredAutoCommit);
			}
		} catch (SQLException e) {
			// Only a very poorly implemented driver would fail here,
			// and there's not much we can do about that.
			throw new TransactionException("Error configuring AutoCommit.  "
					+ "Your driver may not support getAutoCommit() or setAutoCommit(). "
					+ "Requested setting: " + desiredAutoCommit + ".  Cause: " + e, e);
		}
	}

	/**
	 * 设置成自动提交
	 */
	protected void resetAutoCommit() {
		try {
			if (!connection.getAutoCommit()) {
				// MyBatis does not call commit/rollback on a connection if just selects were performed.
				// Some databases start transactions with select statements
				// and they mandate a commit/rollback before closing the connection.
				// A workaround is setting the autocommit to true before closing the connection.
				// Sybase throws an exception here.
				if (log.isDebugEnabled()) {
					log.debug("Resetting autocommit to true on JDBC Connection [" + connection + "]");
				}
				connection.setAutoCommit(true);
			}
		} catch (SQLException e) {
			if (log.isDebugEnabled()) {
				log.debug("Error resetting autocommit to true "
						+ "before closing the connection.  Cause: " + e);
			}
		}
	}

	/**
	 * 获得数据库连接
	 * @throws SQLException
	 */
	protected void openConnection() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("Opening JDBC Connection");
		}
		// 直接数据库连接池 通过事务隔离级别 打开连接
		connection = dataSource.getConnection();
		if (level != null) {
			connection.setTransactionIsolation(level.getLevel());
		}
		setDesiredAutoCommit(autoCommit);
	}

	// null 实现
	@Override
	public Integer getTimeout() throws SQLException {
		return null;
	}

}

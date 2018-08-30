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
package org.apache.ibatis.session;

import java.sql.Connection;

/**
 * Creates an {@link SqlSession} out of a connection or a DataSource
 *
 * @author Clinton Begin
 *
 * SqlSessionFactory
 *
 * 通过连接或者数据源 来获取一个SqlSession
 * 提供了不同的 生成会话 方法
 */
public interface SqlSessionFactory {

	/**
	 * 打开会话,默认无参打开
	 * @return
	 */
	SqlSession openSession();

	/**
	 * 打开会话,并设置数据库提交方式
	 * @param autoCommit
	 * @return
	 */
	SqlSession openSession(boolean autoCommit);

	/**
	 * 通过数据库连接来获取会话
	 * @param connection
	 * @return
	 */
	SqlSession openSession(Connection connection);

	/**
	 * 打开一个指定隔离级别的会话
	 * @param level
	 * @return
	 */
	SqlSession openSession(TransactionIsolationLevel level);

	/**
	 * 执行类型
	 * @param execType
	 * @return
	 */
	SqlSession openSession(ExecutorType execType);

	/**
	 * 执行类型外加自动提交
	 * @param execType
	 * @param autoCommit
	 * @return
	 */
	SqlSession openSession(ExecutorType execType, boolean autoCommit);

	/**
	 * 执行类型,外带隔离级别
	 * @param execType
	 * @param level
	 * @return
	 */
	SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);

	/**
	 * 执行类型外带 连接对象
	 * @param execType
	 * @param connection
	 * @return
	 */
	SqlSession openSession(ExecutorType execType, Connection connection);

	/**
	 * 希望子类 维护一个Configuration 对象
	 * 毕竟这个对象是通过 SqlSessionFactoryBuilder 构建过来的
	 * @return
	 */
	Configuration getConfiguration();

}

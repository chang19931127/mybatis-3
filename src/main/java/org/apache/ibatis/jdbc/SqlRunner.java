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
package org.apache.ibatis.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 * 这是SQL语句的执行类
 * 通过TypeHandler 封装一层java和Mybatis 的映射关系更加灵活
 *
 * 包含了数据库的增删改差
 */
public class SqlRunner {

	/**
	 * 主键把
	 */
	public static final int NO_GENERATED_KEY = Integer.MIN_VALUE + 1001;

	/**
	 * 连接对象
	 */
	private final Connection connection;
	/**
	 * 类型处理器注册类,各种类型处理
	 */
	private final TypeHandlerRegistry typeHandlerRegistry;
	/**
	 * 是否生成key
	 */
	private boolean useGeneratedKeySupport;

	public SqlRunner(Connection connection) {
		this.connection = connection;
		this.typeHandlerRegistry = new TypeHandlerRegistry();
	}

	public void setUseGeneratedKeySupport(boolean useGeneratedKeySupport) {
		this.useGeneratedKeySupport = useGeneratedKeySupport;
	}

	/**
	 * Executes a SELECT statement that returns one row.
	 *
	 * @param sql  The SQL
	 * @param args The arguments to be set on the statement.
	 * @return The row expected.
	 * @throws SQLException If less or more than one row is returned
	 *
	 * 执行一个只有一个返回值的SQL语句
	 * 使用PrepareStatement预编译语句安全快速
	 */
	public Map<String, Object> selectOne(String sql, Object... args) throws SQLException {
		List<Map<String, Object>> results = selectAll(sql, args);
		if (results.size() != 1) {
			throw new SQLException("Statement returned " + results.size() + " results where exactly one (1) was expected.");
		}
		return results.get(0);
	}

	/**
	 * Executes a SELECT statement that returns multiple rows.
	 *
	 * @param sql  The SQL
	 * @param args The arguments to be set on the statement.
	 * @return The list of rows expected.
	 * @throws SQLException If statement preparation or execution fails
	 * prepareStatement 进行操作并且设置参数
	 */
	public List<Map<String, Object>> selectAll(String sql, Object... args) throws SQLException {
		PreparedStatement ps = connection.prepareStatement(sql);
		try {
			setParameters(ps, args);
			// 执行操作
			ResultSet rs = ps.executeQuery();
			return getResults(rs);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				//ignore
			}
		}
	}

	/**
	 * Executes an INSERT statement.
	 *
	 * @param sql  The SQL
	 * @param args The arguments to be set on the statement.
	 * @return The number of rows impacted or BATCHED_RESULTS if the statements are being batched.
	 * @throws SQLException If statement preparation or execution fails
	 *
	 * insert 一样PrepareStatement  外加args
	 */
	public int insert(String sql, Object... args) throws SQLException {
		PreparedStatement ps;
		// 看是否使用自动生成主键
		if (useGeneratedKeySupport) {
			// PrepareStatement中可以设置  ,所以学习一门技术一定先要从使用API开始
			ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		} else {
			ps = connection.prepareStatement(sql);
		}

		try {
			// 同样TypeHandler搭配类型转化
			setParameters(ps, args);
			// 除了select 是 ps.executeQuery(); 其他都是 update
			ps.executeUpdate();
			if (useGeneratedKeySupport) {
				// 一般jdbc的插入操作都是返回一个  插入成功的id
				List<Map<String, Object>> keys = getResults(ps.getGeneratedKeys());
				if (keys.size() == 1) {
					Map<String, Object> key = keys.get(0);
					Iterator<Object> i = key.values().iterator();
					if (i.hasNext()) {
						Object genkey = i.next();
						if (genkey != null) {
							try {
								return Integer.parseInt(genkey.toString());
							} catch (NumberFormatException e) {
								//ignore, no numeric key support
							}
						}
					}
				}
			}
			return NO_GENERATED_KEY;
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				//ignore
			}
		}
	}

	/**
	 * Executes an UPDATE statement.
	 *
	 * @param sql  The SQL
	 * @param args The arguments to be set on the statement.
	 * @return The number of rows impacted or BATCHED_RESULTS if the statements are being batched.
	 * @throws SQLException If statement preparation or execution fails
	 */
	public int update(String sql, Object... args) throws SQLException {
		PreparedStatement ps = connection.prepareStatement(sql);
		try {
			// 通过TypeHandler 记性set
			setParameters(ps, args);
			// 然后执行
			return ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				//ignore
			}
		}
	}

	/**
	 * Executes a DELETE statement.
	 *
	 * @param sql  The SQL
	 * @param args The arguments to be set on the statement.
	 * @return The number of rows impacted or BATCHED_RESULTS if the statements are being batched.
	 * @throws SQLException If statement preparation or execution fails
	 *
	 * 删除操作直接调用的update  可以小兄弟
	 */
	public int delete(String sql, Object... args) throws SQLException {
		return update(sql, args);
	}

	/**
	 * Executes any string as a JDBC Statement.
	 * Good for DDL
	 * 执行DDL   DDL 是数据库表data definition language  CREATE、ALTER、DROP
	 * DML 是数据库操作语句
	 * @param sql The SQL
	 * @throws SQLException If statement preparation or execution fails
	 */
	public void run(String sql) throws SQLException {
		Statement stmt = connection.createStatement();
		try {
			// 简单执行对表的操作 删除表 新建表 修改表
			stmt.execute(sql);
		} finally {
			try {
				stmt.close();
			} catch (SQLException e) {
				//ignore
			}
		}
	}

	/**
	 * 关闭连接
	 */
	public void closeConnection() {
		try {
			connection.close();
		} catch (SQLException e) {
			//ignore
		}
	}

	/**
	 * 将可变参数中的args    set到  preparedStatement语句中
	 * 这里针对args 又封装了一层TypeHandler操作估计是不想使用JDBC默认的
	 * @param ps
	 * @param args
	 * @throws SQLException
	 */
	private void setParameters(PreparedStatement ps, Object... args) throws SQLException {
		// 怎么不用foreach 需要i有作用么
		for (int i = 0, n = args.length; i < n; i++) {
			if (args[i] == null) {
				// 参数不能是null 否则有问题
				throw new SQLException("SqlRunner requires an instance of Null to represent typed null values for JDBC compatibility");
			} else if (args[i] instanceof Null) {
				// 处理器参数 进行操作,封装 PrepareStatement 参数设置
				((Null) args[i]).getTypeHandler().setParameter(ps, i + 1, null, ((Null) args[i]).getJdbcType());
			} else {
				TypeHandler typeHandler = typeHandlerRegistry.getTypeHandler(args[i].getClass());
				if (typeHandler == null) {
					throw new SQLException("SqlRunner could not find a TypeHandler instance for " + args[i].getClass());
				} else {
					typeHandler.setParameter(ps, i + 1, args[i], null);
				}
			}
		}
	}

	/**
	 * 将结果集进行操作返回成List<Map<String, Object> 对象
	 * 通过jdbc返回的结果集 转交给Mybatis通过TypeHandler进行处理
	 * 每一行一个 行名外加一个处理后的结果 Map
	 * 然后放到一个List中
	 * 记着数据库是表,表就是二维 就对应了一维Map存储,一维List存放
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private List<Map<String, Object>> getResults(ResultSet rs) throws SQLException {
		try {
			// 结果集
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
			// jdbc返回的列表集
			List<String> columns = new ArrayList<String>();
			// 对应的TypeHandler
			List<TypeHandler<?>> typeHandlers = new ArrayList<TypeHandler<?>>();
			// 结果援对象
			ResultSetMetaData rsmd = rs.getMetaData();
			for (int i = 0, n = rsmd.getColumnCount(); i < n; i++) {
				// 添加到columns中
				columns.add(rsmd.getColumnLabel(i + 1));
				try {
					// jdbc 返回的结果集类名
					Class<?> type = Resources.classForName(rsmd.getColumnClassName(i + 1));
					// 通过jdbc 的结果集找到我们的 typeHandler
					TypeHandler<?> typeHandler = typeHandlerRegistry.getTypeHandler(type);
					if (typeHandler == null) {
						// 默认就Objects
						typeHandler = typeHandlerRegistry.getTypeHandler(Object.class);
					}
					typeHandlers.add(typeHandler);
				} catch (Exception e) {
					typeHandlers.add(typeHandlerRegistry.getTypeHandler(Object.class));
				}
			}
			// 对结果集进行操作
			while (rs.next()) {
				Map<String, Object> row = new HashMap<String, Object>();
				for (int i = 0, n = columns.size(); i < n; i++) {
					String name = columns.get(i);
					TypeHandler<?> handler = typeHandlers.get(i);
					// 最终存到row中   columns的行名  配合handler通过结果进行操作对象
					row.put(name.toUpperCase(Locale.ENGLISH), handler.getResult(rs, name));
				}
				list.add(row);
			}
			return list;
		} finally {
			// 一定要释放资源
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

}

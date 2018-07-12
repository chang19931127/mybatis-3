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
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 * 类型处理泛型接口
 * 主要是为了 PreparedStatement 进行准备的
 */
public interface TypeHandler<T> {

	/**
	 * 将 parameter 设置到 preparedStatement 中的第几个位置
	 * @param ps PreparedStatement
	 * @param i
	 * @param parameter
	 * @param jdbcType
	 * @throws SQLException
	 */
	void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

	/**
	 * 通过结果集中的column名获取数据
	 * @param rs
	 * @param columnName
	 * @return
	 * @throws SQLException
	 */
	T getResult(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 通过结果集中的index 获取数据
	 * @param rs
	 * @param columnIndex
	 * @return
	 * @throws SQLException
	 */
	T getResult(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 通过 CallableStatement 的 index 来获取数据
	 * 针对存储过程
	 * @param cs
	 * @param columnIndex
	 * @return
	 * @throws SQLException
	 */
	T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}

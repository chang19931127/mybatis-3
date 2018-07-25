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
package org.apache.ibatis.executor.keygen;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 *
 * jdbc生成主键后 做的处理 通过 statement.getGeneratorKeys 来获取key
 */
public class Jdbc3KeyGenerator implements KeyGenerator {

	/**
	 * A shared instance.
	 * @since 3.4.3
	 */
	public static final Jdbc3KeyGenerator INSTANCE = new Jdbc3KeyGenerator();

	@Override
	public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		// do nothing
	}

	@Override
	public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		// 查出结果 的后处理
		processBatch(ms, stmt, getParameters(parameter));
	}

	/**
	 * 批量处理
	 * @param ms
	 * @param stmt
	 * @param parameters
	 */
	public void processBatch(MappedStatement ms, Statement stmt, Collection<Object> parameters) {
		ResultSet rs = null;
		try {
			// 拿到自动生成的key
			rs = stmt.getGeneratedKeys();

			// 工具组开始
			final Configuration configuration = ms.getConfiguration();
			final TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
			final String[] keyProperties = ms.getKeyProperties();
			final ResultSetMetaData rsmd = rs.getMetaData();
			TypeHandler<?>[] typeHandlers = null;

			// 做处理
			if (keyProperties != null && rsmd.getColumnCount() >= keyProperties.length) {
				for (Object parameter : parameters) {
					// there should be one row for each statement (also one for each parameter)
					if (!rs.next()) {
						break;
					}
					final MetaObject metaParam = configuration.newMetaObject(parameter);
					if (typeHandlers == null) {
						typeHandlers = getTypeHandlers(typeHandlerRegistry, metaParam, keyProperties, rsmd);
					}
					// 处理
					populateKeys(rs, metaParam, keyProperties, typeHandlers);
				}
			}
		} catch (Exception e) {
			throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	/**
	 * 参数转化啦.    感觉这个方法不太好. 但是 parameter的类型确实没办法控制
	 * 这里面都对应的是 配置文件里面的内容吧 哈哈哈哈肯定是这样 类似spring 中的 property
	 * @param parameter
	 * @return
	 */
	private Collection<Object> getParameters(Object parameter) {
		Collection<Object> parameters = null;
		if (parameter instanceof Collection) {
			// Collection 转化
			parameters = (Collection) parameter;
		} else if (parameter instanceof Map) {
			// map了
			Map parameterMap = (Map) parameter;
			// map套map
			if (parameterMap.containsKey("collection")) {
				parameters = (Collection) parameterMap.get("collection");
			} else if (parameterMap.containsKey("list")) {
				parameters = (List) parameterMap.get("list");
			} else if (parameterMap.containsKey("array")) {
				parameters = Arrays.asList((Object[]) parameterMap.get("array"));
			}
		}
		// 简单参数就直接 add 到 arrayList里面吧
		if (parameters == null) {
			parameters = new ArrayList<Object>();
			parameters.add(parameter);
		}
		return parameters;
	}

	private TypeHandler<?>[] getTypeHandlers(TypeHandlerRegistry typeHandlerRegistry, MetaObject metaParam, String[] keyProperties, ResultSetMetaData rsmd) throws SQLException {
		TypeHandler<?>[] typeHandlers = new TypeHandler<?>[keyProperties.length];
		for (int i = 0; i < keyProperties.length; i++) {
			if (metaParam.hasSetter(keyProperties[i])) {
				TypeHandler<?> th;
				try {
					Class<?> keyPropertyType = metaParam.getSetterType(keyProperties[i]);
					th = typeHandlerRegistry.getTypeHandler(keyPropertyType, JdbcType.forCode(rsmd.getColumnType(i + 1)));
				} catch (BindingException e) {
					th = null;
				}
				typeHandlers[i] = th;
			}
		}
		return typeHandlers;
	}

	/**
	 * 拉去keys
	 * @param rs
	 * @param metaParam
	 * @param keyProperties
	 * @param typeHandlers
	 * @throws SQLException
	 */
	private void populateKeys(ResultSet rs, MetaObject metaParam, String[] keyProperties, TypeHandler<?>[] typeHandlers) throws SQLException {
		// 直接遍历
		for (int i = 0; i < keyProperties.length; i++) {
			String property = keyProperties[i];
			TypeHandler<?> th = typeHandlers[i];
			if (th != null) {
				// 为什么+1 结果集里面 从 1 开始
				Object value = th.getResult(rs, i + 1);
				// 最后存储的就是 id 和 结果了
				metaParam.setValue(property, value);
			}
		}
	}

}

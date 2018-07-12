/**
 * Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.mapping;

import java.sql.ResultSet;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * 参数映射 对象封装的
 * 会不会一个映射关系对应一个这个类。
 * 内部通过构造器,外带一些校验和自维护 生成ParameterMapping
 * @author Clinton Begin
 */
public class ParameterMapping {

	/**
	 * 配置类不用说。
	 */
	private Configuration configuration;

	/**
	 * 属性
	 */
	private String property;
	/**
	 * 参数模式 in out inout
	 */
	private ParameterMode mode;
	/**
	 * 默认的java类型
	 */
	private Class<?> javaType = Object.class;
	/**
	 * jdbc的类型
	 */
	private JdbcType jdbcType;
	private Integer numericScale;
	/**
	 * 类型的处理
	 */
	private TypeHandler<?> typeHandler;
	private String resultMapId;
	/**
	 * jdbc 类型
	 */
	private String jdbcTypeName;
	/**
	 * 表达式
	 */
	private String expression;

	// 构造方法私有

	private ParameterMapping() {
	}

	/**
	 * 内置一个 建造者来进行操作
	 * 然后通过ParameterMapping.Builder 来进行 构造ParameterMapping
	 * 算是懒加载 ，并且通过内部来构造外部对象,这样的好处是什么
	 * 是为了一个类 完成建造者模式？
	 * 建造者的好处就是为了方便处理逻辑
	 */
	public static class Builder {
		private ParameterMapping parameterMapping = new ParameterMapping();

		public Builder(Configuration configuration, String property, TypeHandler<?> typeHandler) {
			parameterMapping.configuration = configuration;
			parameterMapping.property = property;
			parameterMapping.typeHandler = typeHandler;
			parameterMapping.mode = ParameterMode.IN;
		}

		public Builder(Configuration configuration, String property, Class<?> javaType) {
			parameterMapping.configuration = configuration;
			parameterMapping.property = property;
			parameterMapping.javaType = javaType;
			parameterMapping.mode = ParameterMode.IN;
		}

		public Builder mode(ParameterMode mode) {
			parameterMapping.mode = mode;
			return this;
		}

		public Builder javaType(Class<?> javaType) {
			parameterMapping.javaType = javaType;
			return this;
		}

		public Builder jdbcType(JdbcType jdbcType) {
			parameterMapping.jdbcType = jdbcType;
			return this;
		}

		public Builder numericScale(Integer numericScale) {
			parameterMapping.numericScale = numericScale;
			return this;
		}

		public Builder resultMapId(String resultMapId) {
			parameterMapping.resultMapId = resultMapId;
			return this;
		}

		public Builder typeHandler(TypeHandler<?> typeHandler) {
			parameterMapping.typeHandler = typeHandler;
			return this;
		}

		public Builder jdbcTypeName(String jdbcTypeName) {
			parameterMapping.jdbcTypeName = jdbcTypeName;
			return this;
		}

		public Builder expression(String expression) {
			parameterMapping.expression = expression;
			return this;
		}

		/**
		 * 主要还是这个核心方法 build
		 * @return
		 */
		public ParameterMapping build() {
			resolveTypeHandler();
			validate();
			return parameterMapping;
		}

		/**
		 * 校验操作
		 */
		private void validate() {
			// resultSet是java类型 一定要存在对应的 id
			if (ResultSet.class.equals(parameterMapping.javaType)) {
				if (parameterMapping.resultMapId == null) {
					throw new IllegalStateException("Missing resultmap in property '"
							+ parameterMapping.property + "'.  "
							+ "Parameters of type java.sql.ResultSet require a resultmap.");
				}
			} else {
				// 或者没有对应的 java类型处理 就异常啊
				if (parameterMapping.typeHandler == null) {
					throw new IllegalStateException("Type handler was null on parameter mapping for property '"
							+ parameterMapping.property + "'. It was either not specified and/or could not be found for the javaType ("
							+ parameterMapping.javaType.getName() + ") : jdbcType (" + parameterMapping.jdbcType + ") combination.");
				}
			}
		}

		/**
		 * 执行发现注册TypeHandler
		 */
		private void resolveTypeHandler() {
			// 只有javaType 的情况下   通过javaType 从Configuration中寻找TypeHandler对象
			if (parameterMapping.typeHandler == null && parameterMapping.javaType != null) {
				Configuration configuration = parameterMapping.configuration;
				TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
				parameterMapping.typeHandler = typeHandlerRegistry.getTypeHandler(parameterMapping.javaType, parameterMapping.jdbcType);
			}
		}

	}

	public String getProperty() {
		return property;
	}

	/**
	 * Used for handling output of callable statements
	 * @return
	 */
	public ParameterMode getMode() {
		return mode;
	}

	/**
	 * Used for handling output of callable statements
	 * @return
	 */
	public Class<?> getJavaType() {
		return javaType;
	}

	/**
	 * Used in the UnknownTypeHandler in case there is no handler for the property type
	 * @return
	 */
	public JdbcType getJdbcType() {
		return jdbcType;
	}

	/**
	 * Used for handling output of callable statements
	 * @return
	 */
	public Integer getNumericScale() {
		return numericScale;
	}

	/**
	 * Used when setting parameters to the PreparedStatement
	 * @return
	 */
	public TypeHandler<?> getTypeHandler() {
		return typeHandler;
	}

	/**
	 * Used for handling output of callable statements
	 * @return
	 */
	public String getResultMapId() {
		return resultMapId;
	}

	/**
	 * Used for handling output of callable statements
	 * @return
	 */
	public String getJdbcTypeName() {
		return jdbcTypeName;
	}

	/**
	 * Not used
	 * @return
	 */
	public String getExpression() {
		return expression;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ParameterMapping{");
		//sb.append("configuration=").append(configuration); // configuration doesn't have a useful .toString()
		sb.append("property='").append(property).append('\'');
		sb.append(", mode=").append(mode);
		sb.append(", javaType=").append(javaType);
		sb.append(", jdbcType=").append(jdbcType);
		sb.append(", numericScale=").append(numericScale);
		//sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't have a useful .toString()
		sb.append(", resultMapId='").append(resultMapId).append('\'');
		sb.append(", jdbcTypeName='").append(jdbcTypeName).append('\'');
		sb.append(", expression='").append(expression).append('\'');
		sb.append('}');
		return sb.toString();
	}
}

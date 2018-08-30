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
package org.apache.ibatis.scripting.xmltags;

import java.util.HashMap;
import java.util.Map;

import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 * mybatis 讲究动态Sql 这是一个动态上下文
 * 封装了两个东西,一个ContextMap 一个是SqlBuilder
 * ContextMap 有封装了一个Map 和所有key的类元信息
 */
public class DynamicContext {

	public static final String PARAMETER_OBJECT_KEY = "_parameter";
	public static final String DATABASE_ID_KEY = "_databaseId";

	static {
		// 应该是直接交给 ognl 去操作了
		OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
	}

	private final ContextMap bindings;
	private final StringBuilder sqlBuilder = new StringBuilder();
	private int uniqueNumber = 0;

	/**
	 * 构造方法
	 * @param configuration
	 * @param parameterObject
	 */
	public DynamicContext(Configuration configuration, Object parameterObject) {
		if (parameterObject != null && !(parameterObject instanceof Map)) {
			MetaObject metaObject = configuration.newMetaObject(parameterObject);
			bindings = new ContextMap(metaObject);
		} else {
			bindings = new ContextMap(null);
		}
		// 绑定 参数和 数据库id
		bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
		bindings.put(DATABASE_ID_KEY, configuration.getDatabaseId());
	}

	public Map<String, Object> getBindings() {
		return bindings;
	}

	public void bind(String name, Object value) {
		bindings.put(name, value);
	}

	public void appendSql(String sql) {
		sqlBuilder.append(sql);
		sqlBuilder.append(" ");
	}

	public String getSql() {
		return sqlBuilder.toString().trim();
	}

	public int getUniqueNumber() {
		return uniqueNumber++;
	}

	/**
	 * 内部静态类 是一个mao
	 * 可以通过这个类 拿到key 对应的原始信息 可以去反射啊各种
	 */
	static class ContextMap extends HashMap<String, Object> {
		private static final long serialVersionUID = 2977601501966151582L;
		/**
		 * 就比 map 多一个 MetaObject 对象元信息把
		 */
		private MetaObject parameterMetaObject;

		public ContextMap(MetaObject parameterMetaObject) {
			this.parameterMetaObject = parameterMetaObject;
		}

		@Override
		public Object get(Object key) {
			String strKey = (String) key;
			if (super.containsKey(strKey)) {
				return super.get(strKey);
			}

			if (parameterMetaObject != null) {
				// issue #61 do not modify the context when reading
				return parameterMetaObject.getValue(strKey);
			}

			return null;
		}
	}

	/**
	 * Context 访问 ognl属性访问
	 * 这应该类似一个工具类
	 * 仅对方法的参数做操作
	 * 就是map key value
	 */
	static class ContextAccessor implements PropertyAccessor {

		/**
		 * 直接通过 target 然后拿到name 对应的key
		 * @param context
		 * @param target
		 * @param name
		 * @return
		 * @throws OgnlException
		 */
		@Override
		public Object getProperty(Map context, Object target, Object name)
				throws OgnlException {
			Map map = (Map) target;

			Object result = map.get(name);
			if (map.containsKey(name) || result != null) {
				return result;
			}

			Object parameterObject = map.get(PARAMETER_OBJECT_KEY);
			if (parameterObject instanceof Map) {
				return ((Map) parameterObject).get(name);
			}

			return null;
		}

		/**
		 * 将 name 和 value put 到 target 中
		 * @param context
		 * @param target
		 * @param name
		 * @param value
		 * @throws OgnlException
		 */
		@Override
		public void setProperty(Map context, Object target, Object name, Object value)
				throws OgnlException {
			Map<Object, Object> map = (Map<Object, Object>) target;
			map.put(name, value);
		}

		@Override
		public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
			return null;
		}

		@Override
		public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
			return null;
		}
	}
}
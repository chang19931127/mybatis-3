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
package org.apache.ibatis.binding;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.Jdk;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.OptionalUtil;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 * @author Kazuki Shimizu
 * 直接是 Mapper类的method 对应的对象
 * Mapper 的 method 会被解析成sql
 */
public class MapperMethod {

	/**
	 * Sql 命令
	 */
	private final SqlCommand command;
	/**
	 * 方法签名
	 */
	private final MethodSignature method;

	public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
		// 直接构造 config 接口 方法
		this.command = new SqlCommand(config, mapperInterface, method);
		this.method = new MethodSignature(config, mapperInterface, method);
	}

	/**
	 * 重点使这个方法 代理接口后 调用方法 会执行这个方法
	 * @param sqlSession SqlSession对象
	 * @param args 方法传入的参数
	 * @return
	 * 主要通过SqlSession 通过命令模式来进行操作
	 */
	public Object execute(SqlSession sqlSession, Object[] args) {
		Object result;
		switch (command.getType()) {
			case INSERT: {
				Object param = method.convertArgsToSqlCommandParam(args);
				// 然后执行操作了
				result = rowCountResult(sqlSession.insert(command.getName(), param));
				break;
			}
			case UPDATE: {
				Object param = method.convertArgsToSqlCommandParam(args);
				result = rowCountResult(sqlSession.update(command.getName(), param));
				break;
			}
			case DELETE: {
				Object param = method.convertArgsToSqlCommandParam(args);
				result = rowCountResult(sqlSession.delete(command.getName(), param));
				break;
			}
			case SELECT:
				// 查询的情况是最多的
				if (method.returnsVoid() && method.hasResultHandler()) {
					// void 返回 并且 还有结果处理的
					executeWithResultHandler(sqlSession, args);
					result = null;
				} else if (method.returnsMany()) {
					// 多结果返回
					result = executeForMany(sqlSession, args);
				} else if (method.returnsMap()) {
					// Map返回
					result = executeForMap(sqlSession, args);
				} else if (method.returnsCursor()) {
					// 游标返回
					result = executeForCursor(sqlSession, args);
				} else {
					// 单结果集返回的
					// 会做一些空处理
					Object param = method.convertArgsToSqlCommandParam(args);
					result = sqlSession.selectOne(command.getName(), param);
					if (method.returnsOptional() &&
							(result == null || !method.getReturnType().equals(result.getClass()))) {
						result = OptionalUtil.ofNullable(result);
					}
				}
				break;
			case FLUSH:
				result = sqlSession.flushStatements();
				break;
			default:
				throw new BindingException("Unknown execution method for: " + command.getName());
		}
		// null 返回 碰到原生类型就要报错了  哈哈哈
		if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
			throw new BindingException("Mapper method '" + command.getName()
					+ " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
		}
		return result;
	}

	/**
	 * 计算结果集
	 * @param rowCount
	 * @return
	 */
	private Object rowCountResult(int rowCount) {
		final Object result;
		if (method.returnsVoid()) {
			result = null;
		} else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
			result = rowCount;
		} else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
			result = (long) rowCount;
		} else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
			result = rowCount > 0;
		} else {
			throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
		}
		return result;
	}

	/**
	 * 针对 void 方法 并且有处理操作的
	 * @param sqlSession
	 * @param args
	 */
	private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
		MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
		if (!StatementType.CALLABLE.equals(ms.getStatementType())
				&& void.class.equals(ms.getResultMaps().get(0).getType())) {
			throw new BindingException("method " + command.getName()
					+ " needs either a @ResultMap annotation, a @ResultType annotation,"
					+ " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
		}
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			// 方法有分页操作
			RowBounds rowBounds = method.extractRowBounds(args);
			sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
		} else {
			sqlSession.select(command.getName(), param, method.extractResultHandler(args));
		}
	}

	/**
	 * 多结果及返回的    使用selectList api
	 * @param sqlSession
	 * @param args
	 * @param <E>
	 * @return
	 */
	private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
		List<E> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.<E>selectList(command.getName(), param, rowBounds);
		} else {
			result = sqlSession.<E>selectList(command.getName(), param);
		}
		// issue #510 Collections & arrays support
		if (!method.getReturnType().isAssignableFrom(result.getClass())) {
			if (method.getReturnType().isArray()) {
				return convertToArray(result);
			} else {
				return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
			}
		}
		return result;
	}

	/**
	 * 关于Cursor 执行游标的
	 * @param sqlSession
	 * @param args
	 * @param <T>
	 * @return
	 */
	private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
		Cursor<T> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.<T>selectCursor(command.getName(), param, rowBounds);
		} else {
			result = sqlSession.<T>selectCursor(command.getName(), param);
		}
		return result;
	}

	private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
		Object collection = config.getObjectFactory().create(method.getReturnType());
		MetaObject metaObject = config.newMetaObject(collection);
		metaObject.addAll(list);
		return collection;
	}

	@SuppressWarnings("unchecked")
	private <E> Object convertToArray(List<E> list) {
		Class<?> arrayComponentType = method.getReturnType().getComponentType();
		Object array = Array.newInstance(arrayComponentType, list.size());
		if (arrayComponentType.isPrimitive()) {
			for (int i = 0; i < list.size(); i++) {
				Array.set(array, i, list.get(i));
			}
			return array;
		} else {
			return list.toArray((E[]) array);
		}
	}

	/**
	 * 返回Map的类型 直接selectMap ap调用
	 * @param sqlSession
	 * @param args
	 * @param <K>
	 * @param <V>
	 * @return
	 */
	private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
		Map<K, V> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey(), rowBounds);
		} else {
			result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey());
		}
		return result;
	}

	public static class ParamMap<V> extends HashMap<String, V> {

		private static final long serialVersionUID = -2212268410512043556L;

		@Override
		public V get(Object key) {
			if (!super.containsKey(key)) {
				throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
			}
			return super.get(key);
		}

	}

	/**
	 * Sql 命令封装对象
	 * 整体会涉及到mapping 中的内容
	 */
	public static class SqlCommand {
		/**
		 * Name
		 */
		private final String name;
		/**
		 * 执行类型 增删改查
		 */
		private final SqlCommandType type;

		public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
			// 主要是构造方法
			final String methodName = method.getName();
			final Class<?> declaringClass = method.getDeclaringClass();
			// 通过接口,方法,声明类,配置来获取MapperStatement
			MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass,
					configuration);
			if (ms == null) {
				if (method.getAnnotation(Flush.class) != null) {
					name = null;
					type = SqlCommandType.FLUSH;
				} else {
					throw new BindingException("Invalid bound statement (not found): "
							+ mapperInterface.getName() + "." + methodName);
				}
			} else {
				name = ms.getId();
				type = ms.getSqlCommandType();
				if (type == SqlCommandType.UNKNOWN) {
					throw new BindingException("Unknown execution method for: " + name);
				}
			}
		}

		public String getName() {
			return name;
		}

		public SqlCommandType getType() {
			return type;
		}

		/**
		 * 获取MappedStatement 对象
		 * @param mapperInterface
		 * @param methodName
		 * @param declaringClass
		 * @param configuration
		 * @return
		 */
		private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
		                                               Class<?> declaringClass, Configuration configuration) {
			// 直接 xxx.xxx.method
			String statementId = mapperInterface.getName() + "." + methodName;
			// 配置中是否有配置 这就接口和Mapper 结合起来了
			if (configuration.hasStatement(statementId)) {
				return configuration.getMappedStatement(statementId);
			} else if (mapperInterface.equals(declaringClass)) {
				return null;
			}
			for (Class<?> superInterface : mapperInterface.getInterfaces()) {
				// 将所有的接口 都进行递归操作.直到获取到method 对应的 MappedStatement
				if (declaringClass.isAssignableFrom(superInterface)) {
					MappedStatement ms = resolveMappedStatement(superInterface, methodName,
							declaringClass, configuration);
					if (ms != null) {
						return ms;
					}
				}
			}
			return null;
		}
	}

	/**
	 * 自己封装了一个方法对象
	 */
	public static class MethodSignature {
		/**
		 * 是否多结果返回
		 */
		private final boolean returnsMany;
		/**
		 * 是否返回Map
		 */
		private final boolean returnsMap;
		/**
		 * 是否 void 返回
		 */
		private final boolean returnsVoid;
		private final boolean returnsCursor;
		private final boolean returnsOptional;
		private final Class<?> returnType;
		private final String mapKey;
		private final Integer resultHandlerIndex;
		private final Integer rowBoundsIndex;
		private final ParamNameResolver paramNameResolver;

		public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
			Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
			if (resolvedReturnType instanceof Class<?>) {
				this.returnType = (Class<?>) resolvedReturnType;
			} else if (resolvedReturnType instanceof ParameterizedType) {
				this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
			} else {
				this.returnType = method.getReturnType();
			}
			this.returnsVoid = void.class.equals(this.returnType);
			this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
			this.returnsCursor = Cursor.class.equals(this.returnType);
			this.returnsOptional = Jdk.optionalExists && Optional.class.equals(this.returnType);
			this.mapKey = getMapKey(method);
			this.returnsMap = this.mapKey != null;
			this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
			this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
			this.paramNameResolver = new ParamNameResolver(configuration, method);
		}

		/**
		 * 直接将参数 转化成SqlCommand 参数
		 * @param args
		 * @return
		 */
		public Object convertArgsToSqlCommandParam(Object[] args) {
			return paramNameResolver.getNamedParams(args);
		}

		public boolean hasRowBounds() {
			return rowBoundsIndex != null;
		}

		/**
		 * 获得偏移量操作
		 * @param args
		 * @return
		 */
		public RowBounds extractRowBounds(Object[] args) {
			return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
		}

		public boolean hasResultHandler() {
			return resultHandlerIndex != null;
		}

		public ResultHandler extractResultHandler(Object[] args) {
			return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
		}

		public String getMapKey() {
			return mapKey;
		}

		public Class<?> getReturnType() {
			return returnType;
		}

		public boolean returnsMany() {
			return returnsMany;
		}

		public boolean returnsMap() {
			return returnsMap;
		}

		public boolean returnsVoid() {
			return returnsVoid;
		}

		public boolean returnsCursor() {
			return returnsCursor;
		}

		/**
		 * return whether return type is {@code java.util.Optional}
		 * @return return {@code true}, if return type is {@code java.util.Optional}
		 * @since 3.5.0
		 */
		public boolean returnsOptional() {
			return returnsOptional;
		}

		private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
			Integer index = null;
			final Class<?>[] argTypes = method.getParameterTypes();
			for (int i = 0; i < argTypes.length; i++) {
				if (paramType.isAssignableFrom(argTypes[i])) {
					if (index == null) {
						index = i;
					} else {
						throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
					}
				}
			}
			return index;
		}

		private String getMapKey(Method method) {
			String mapKey = null;
			if (Map.class.isAssignableFrom(method.getReturnType())) {
				final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
				if (mapKeyAnnotation != null) {
					mapKey = mapKeyAnnotation.value();
				}
			}
			return mapKey;
		}
	}

}

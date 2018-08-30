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
package org.apache.ibatis.executor.loader;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BaseExecutor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * @author Clinton Begin
 * @author Franta Mejta
 * 结果加载 Map
 */
public class ResultLoaderMap {

	/**
	 * 封装一个 对象    属性名 非加载的类型
	 */
	private final Map<String, LoadPair> loaderMap = new HashMap<String, LoadPair>();

	public void addLoader(String property, MetaObject metaResultObject, ResultLoader resultLoader) {
		String upperFirst = getUppercaseFirstProperty(property);
		if (!upperFirst.equalsIgnoreCase(property) && loaderMap.containsKey(upperFirst)) {
			throw new ExecutorException("Nested lazy loaded result property '" + property +
					"' for query id '" + resultLoader.mappedStatement.getId() +
					" already exists in the result map. The leftmost property of all lazy loaded properties must be unique within a result map.");
		}
		loaderMap.put(upperFirst, new LoadPair(property, metaResultObject, resultLoader));
	}

	public final Map<String, LoadPair> getProperties() {
		return new HashMap<String, LoadPair>(this.loaderMap);
	}

	public Set<String> getPropertyNames() {
		return loaderMap.keySet();
	}

	public int size() {
		return loaderMap.size();
	}

	public boolean hasLoader(String property) {
		return loaderMap.containsKey(property.toUpperCase(Locale.ENGLISH));
	}

	public boolean load(String property) throws SQLException {
		LoadPair pair = loaderMap.remove(property.toUpperCase(Locale.ENGLISH));
		if (pair != null) {
			pair.load();
			return true;
		}
		return false;
	}

	public void remove(String property) {
		loaderMap.remove(property.toUpperCase(Locale.ENGLISH));
	}

	public void loadAll() throws SQLException {
		final Set<String> methodNameSet = loaderMap.keySet();
		String[] methodNames = methodNameSet.toArray(new String[methodNameSet.size()]);
		for (String methodName : methodNames) {
			load(methodName);
		}
	}

	private static String getUppercaseFirstProperty(String property) {
		String[] parts = property.split("\\.");
		return parts[0].toUpperCase(Locale.ENGLISH);
	}

	/**
	 * Property which was not loaded yet.
	 * 尚未装载的属性
	 * 这里为什么需要序列化啊
	 */
	public static class LoadPair implements Serializable {

		private static final long serialVersionUID = 20130412;
		/**
		 * Name of factory method which returns database connection.
		 * 常量
		 */
		private static final String FACTORY_METHOD = "getConfiguration";
		/**
		 * Object to check whether we went through serialization..
		 */
		private final transient Object serializationCheck = new Object();
		/**
		 * Meta object which sets loaded properties.
		 * resultObject 的元对象
		 */
		private transient MetaObject metaResultObject;
		/**
		 * Result loader which loads unread properties.
		 * 结果加载
		 */
		private transient ResultLoader resultLoader;
		/**
		 * Wow, logger.
		 */
		private transient Log log;
		/**
		 * Factory class through which we get database connection.
		 */
		private Class<?> configurationFactory;
		/**
		 * Name of the unread property.
		 */
		private String property;
		/**
		 * ID of SQL statement which loads the property.
		 */
		private String mappedStatement;
		/**
		 * Parameter of the sql statement.
		 */
		private Serializable mappedParameter;

		private LoadPair(final String property, MetaObject metaResultObject, ResultLoader resultLoader) {
			// 根据是否可以序列化，来设置一些属性
			// 属性
			this.property = property;
			// ResultObject
			this.metaResultObject = metaResultObject;
			// ResultLoader
			this.resultLoader = resultLoader;

      /* Save required information only if original object can be serialized. */
            // 如果 metaResultObject 可以序列化
			if (metaResultObject != null && metaResultObject.getOriginalObject() instanceof Serializable) {
				// 通过ResultLoader 获取到 参数对象
				final Object mappedStatementParameter = resultLoader.parameterObject;

        /* @todo May the parameter be null? */
				if (mappedStatementParameter instanceof Serializable) {
					// 参数对象可以序列化 set
					this.mappedStatement = resultLoader.mappedStatement.getId();
					this.mappedParameter = (Serializable) mappedStatementParameter;

					this.configurationFactory = resultLoader.configuration.getConfigurationFactory();
				} else {
					Log log = this.getLogger();
					if (log.isDebugEnabled()) {
						log.debug("Property [" + this.property + "] of ["
								+ metaResultObject.getOriginalObject().getClass() + "] cannot be loaded "
								+ "after deserialization. Make sure it's loaded before serializing "
								+ "forenamed object.");
					}
				}
			}
		}

		public void load() throws SQLException {
	  /* These field should not be null unless the loadpair was serialized.
       * Yet in that case this method should not be called. */
			if (this.metaResultObject == null) {
				throw new IllegalArgumentException("metaResultObject is null");
			}
			if (this.resultLoader == null) {
				throw new IllegalArgumentException("resultLoader is null");
			}

			this.load(null);
		}

		/**
		 * 未加载的进行加载
		 * @param userObject
		 * @throws SQLException
		 */
		public void load(final Object userObject) throws SQLException {
			// metaResultObject 或者 resultLoader 两者其中之一一个null
			// 就要通过 configuration 和mappedStatement 来将这两个进行填充
			if (this.metaResultObject == null || this.resultLoader == null) {
				// mappedParameter 不能为null
				if (this.mappedParameter == null) {
					throw new ExecutorException("Property [" + this.property + "] cannot be loaded because "
							+ "required parameter of mapped statement ["
							+ this.mappedStatement + "] is not serializable.");
				}

				final Configuration config = this.getConfiguration();
				final MappedStatement ms = config.getMappedStatement(this.mappedStatement);
				if (ms == null) {
					throw new ExecutorException("Cannot lazy load property [" + this.property
							+ "] of deserialized object [" + userObject.getClass()
							+ "] because configuration does not contain statement ["
							+ this.mappedStatement + "]");
				}

				// 通过 userObject 得到 metaResultObject
				this.metaResultObject = config.newMetaObject(userObject);
				this.resultLoader = new ResultLoader(config, new ClosedExecutor(), ms, this.mappedParameter,
						metaResultObject.getSetterType(this.property), null, null);
			}

      /* We are using a new executor because we may be (and likely are) on a new thread
       * and executors aren't thread safe. (Is this sufficient?)
       *
       * A better approach would be making executors thread safe. */

			// 这个对象 不可能反序列化的 做一个检查 然后做一个搭配
			if (this.serializationCheck == null) {
				final ResultLoader old = this.resultLoader;
				this.resultLoader = new ResultLoader(old.configuration, new ClosedExecutor(), old.mappedStatement,
						old.parameterObject, old.targetType, old.cacheKey, old.boundSql);
			}

			this.metaResultObject.setValue(property, this.resultLoader.loadResult());
		}

		/**
		 * getConfiguration
		 * 也就是常量 会反射调用的方法
		 * @return
		 */
		private Configuration getConfiguration() {
			if (this.configurationFactory == null) {
				throw new ExecutorException("Cannot get Configuration as configuration factory was not set.");
			}

			Object configurationObject = null;
			try {
				// 通过 configurationFactory 调用 getConfiguration方法
				final Method factoryMethod = this.configurationFactory.getDeclaredMethod(FACTORY_METHOD);
				if (!Modifier.isStatic(factoryMethod.getModifiers())) {
					throw new ExecutorException("Cannot get Configuration as factory method ["
							+ this.configurationFactory + "]#["
							+ FACTORY_METHOD + "] is not static.");
				}

				if (!factoryMethod.isAccessible()) {
					configurationObject = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						@Override
						public Object run() throws Exception {
							try {
								factoryMethod.setAccessible(true);
								return factoryMethod.invoke(null);
							} finally {
								factoryMethod.setAccessible(false);
							}
						}
					});
				} else {
					// 执行 然后获得对象
					configurationObject = factoryMethod.invoke(null);
				}
			} catch (final ExecutorException ex) {
				throw ex;
			} catch (final NoSuchMethodException ex) {
				throw new ExecutorException("Cannot get Configuration as factory class ["
						+ this.configurationFactory + "] is missing factory method of name ["
						+ FACTORY_METHOD + "].", ex);
			} catch (final PrivilegedActionException ex) {
				throw new ExecutorException("Cannot get Configuration as factory method ["
						+ this.configurationFactory + "]#["
						+ FACTORY_METHOD + "] threw an exception.", ex.getCause());
			} catch (final Exception ex) {
				throw new ExecutorException("Cannot get Configuration as factory method ["
						+ this.configurationFactory + "]#["
						+ FACTORY_METHOD + "] threw an exception.", ex);
			}

			if (!(configurationObject instanceof Configuration)) {
				throw new ExecutorException("Cannot get Configuration as factory method ["
						+ this.configurationFactory + "]#["
						+ FACTORY_METHOD + "] didn't return [" + Configuration.class + "] but ["
						+ (configurationObject == null ? "null" : configurationObject.getClass()) + "].");
			}

			// 这种方式转化对象和 直接 强转有什么区别
//			return (Configuration)configurationObject;
			return Configuration.class.cast(configurationObject);
		}

		private Log getLogger() {
			if (this.log == null) {
				this.log = LogFactory.getLog(this.getClass());
			}
			return this.log;
		}
	}

	/**
	 * CloseExecutor 执行的
	 * 不支持执行器
	 * 已经关闭的执行器
	 */
	private static final class ClosedExecutor extends BaseExecutor {

		public ClosedExecutor() {
			super(null, null);
		}

		@Override
		public boolean isClosed() {
			return true;
		}

		@Override
		protected int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		protected List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		protected <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
			throw new UnsupportedOperationException("Not supported.");
		}
	}
}

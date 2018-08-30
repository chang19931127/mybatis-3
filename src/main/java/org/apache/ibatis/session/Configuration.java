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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.builder.xml.XMLStatementBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.datasource.jndi.JndiDataSourceFactory;
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ReuseExecutor;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.executor.loader.cglib.CglibProxyFactory;
import org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.InterceptorChain;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.LanguageDriverRegistry;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 *         无敌大心脏
 *         究极配置类
 *         这个类连接了整个Mybatis
 *         SqlSessionFactoryBuilder 读取配置文件后 XMLConfigBuilder 构造这个对象
 *         这个东西一身学问
 */
public class Configuration {

	/**
	 * 环境对象
	 * set 或者 构造函数都可以获取
	 */
	private Environment environment;

	// 下面这些属性都很重要对应到xml 配置文件 然后对应到某些功能

	/**
	 * 安全行限制 是否启动
	 */
	private boolean safeRowBoundsEnabled;

	/**
	 * 安全地结果处理是否开启
	 */
	private boolean safeResultHandlerEnabled = true;

	/**
	 * 是否 映射 _ -> 驼峰
	 */
	private boolean mapUnderscoreToCamelCase;

	/**
	 * 是否一致懒加载
	 */
	private boolean aggressiveLazyLoading;

	/**
	 * 是否多结果集开启
	 */
	private boolean multipleResultSetsEnabled = true;

	/**
	 * 是否使用自动生成key
	 */
	private boolean useGeneratedKeys;

	/**
	 * 使用行标签
	 */
	private boolean useColumnLabel = true;

	/**
	 * 是否开启缓存
	 */
	private boolean cacheEnabled = true;

	/**
	 * null处理
	 */
	private boolean callSettersOnNulls;

	/**
	 * 使用实际参数name
	 */
	private boolean useActualParamName = true;

	/**
	 * 空结果返回实例
	 */
	private boolean returnInstanceForEmptyRow;

	/**
	 * 日志前缀
	 */
	private String logPrefix;

	private Class<? extends Log> logImpl;
	private Class<? extends VFS> vfsImpl;

	/**
	 * 本地缓存域
	 */
	private LocalCacheScope localCacheScope = LocalCacheScope.SESSION;
	private JdbcType jdbcTypeForNull = JdbcType.OTHER;
	private Set<String> lazyLoadTriggerMethods = new HashSet<String>(Arrays.asList(new String[]{"equals", "clone", "hashCode", "toString"}));

	/**
	 * 超时时间
	 */
	private Integer defaultStatementTimeout;

	/**
	 * fetchSize  拉取数量
	 */
	private Integer defaultFetchSize;

	/**
	 * 执行
	 */
	private ExecutorType defaultExecutorType = ExecutorType.SIMPLE;

	/**
	 * 映射行为
	 */
	private AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;
	private AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior = AutoMappingUnknownColumnBehavior.NONE;
	private Properties variables = new Properties();

	/**
	 * 和反射有关的工厂
	 */
	private ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
	private ObjectFactory objectFactory = new DefaultObjectFactory();
	private ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();
	private boolean lazyLoadingEnabled = false;

	// #224 Using internal Javassist instead of OGNL

	/**
	 * 使用了   javassist代理技术   这个应该是字节码把
	 */
	private ProxyFactory proxyFactory = new JavassistProxyFactory();
	private String databaseId;
	/**
	 * Configuration factory class.
	 * Used to create Configuration for loading deserialized unread properties.
	 *
	 * @see <a href='https://code.google.com/p/mybatis/issues/detail?id=300'>Issue 300 (google code)</a>
	 */
	private Class<?> configurationFactory;

	// 注册一对东西

	private final MapperRegistry mapperRegistry = new MapperRegistry(this);
	private final InterceptorChain interceptorChain = new InterceptorChain();
	private final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
	private final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
	private final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();

	// strictMap 来根据simpleName 对应的内容 来存储

	private final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>("Mapped Statements collection");
	private final Map<String, Cache> caches = new StrictMap<Cache>("Caches collection");
	private final Map<String, ResultMap> resultMaps = new StrictMap<ResultMap>("Result Maps collection");
	private final Map<String, ParameterMap> parameterMaps = new StrictMap<ParameterMap>("Parameter Maps collection");
	private final Map<String, KeyGenerator> keyGenerators = new StrictMap<KeyGenerator>("Key Generators collection");
	private final Set<String> loadedResources = new HashSet<String>();
	private final Map<String, XNode> sqlFragments = new StrictMap<XNode>("XML fragments parsed from previous mappers");

	// 一些处理

	private final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<XMLStatementBuilder>();
	private final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<CacheRefResolver>();
	private final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList<ResultMapResolver>();
	private final Collection<MethodResolver> incompleteMethods = new LinkedList<MethodResolver>();

	/**
	 * A map holds cache-ref relationship. The key is the namespace that
	 * references a cache bound to another namespace and the value is the
	 * namespace which the actual cache is bound to.
	 */
	protected final Map<String, String> cacheRefMap = new HashMap<String, String>();

	public Configuration(Environment environment) {
		this();
		this.environment = environment;
	}

	public Configuration() {
		// 类型别名注册
		typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
		typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);

		typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
		typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
		typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);

		typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
		typeAliasRegistry.registerAlias("FIFO", FifoCache.class);
		typeAliasRegistry.registerAlias("LRU", LruCache.class);
		typeAliasRegistry.registerAlias("SOFT", SoftCache.class);
		typeAliasRegistry.registerAlias("WEAK", WeakCache.class);

		typeAliasRegistry.registerAlias("DB_VENDOR", VendorDatabaseIdProvider.class);

		typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);
		typeAliasRegistry.registerAlias("RAW", RawLanguageDriver.class);

		typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
		typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
		typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
		typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
		typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
		typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
		typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);

		typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
		typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);

		languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
		languageRegistry.register(RawLanguageDriver.class);
	}

	// 一大堆get set

	public String getLogPrefix() {
		return logPrefix;
	}

	public void setLogPrefix(String logPrefix) {
		this.logPrefix = logPrefix;
	}

	public Class<? extends Log> getLogImpl() {
		return logImpl;
	}

	public void setLogImpl(Class<? extends Log> logImpl) {
		if (logImpl != null) {
			this.logImpl = logImpl;
			LogFactory.useCustomLogging(this.logImpl);
		}
	}

	public Class<? extends VFS> getVfsImpl() {
		return this.vfsImpl;
	}

	public void setVfsImpl(Class<? extends VFS> vfsImpl) {
		if (vfsImpl != null) {
			this.vfsImpl = vfsImpl;
			VFS.addImplClass(this.vfsImpl);
		}
	}

	public boolean isCallSettersOnNulls() {
		return callSettersOnNulls;
	}

	public void setCallSettersOnNulls(boolean callSettersOnNulls) {
		this.callSettersOnNulls = callSettersOnNulls;
	}

	public boolean isUseActualParamName() {
		return useActualParamName;
	}

	public void setUseActualParamName(boolean useActualParamName) {
		this.useActualParamName = useActualParamName;
	}

	public boolean isReturnInstanceForEmptyRow() {
		return returnInstanceForEmptyRow;
	}

	public void setReturnInstanceForEmptyRow(boolean returnEmptyInstance) {
		this.returnInstanceForEmptyRow = returnEmptyInstance;
	}

	public String getDatabaseId() {
		return databaseId;
	}

	public void setDatabaseId(String databaseId) {
		this.databaseId = databaseId;
	}

	public Class<?> getConfigurationFactory() {
		return configurationFactory;
	}

	public void setConfigurationFactory(Class<?> configurationFactory) {
		this.configurationFactory = configurationFactory;
	}

	public boolean isSafeResultHandlerEnabled() {
		return safeResultHandlerEnabled;
	}

	public void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled) {
		this.safeResultHandlerEnabled = safeResultHandlerEnabled;
	}

	public boolean isSafeRowBoundsEnabled() {
		return safeRowBoundsEnabled;
	}

	public void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled) {
		this.safeRowBoundsEnabled = safeRowBoundsEnabled;
	}

	public boolean isMapUnderscoreToCamelCase() {
		return mapUnderscoreToCamelCase;
	}

	public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
		this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
	}

	public void addLoadedResource(String resource) {
		loadedResources.add(resource);
	}

	public boolean isResourceLoaded(String resource) {
		return loadedResources.contains(resource);
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public AutoMappingBehavior getAutoMappingBehavior() {
		return autoMappingBehavior;
	}

	public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior) {
		this.autoMappingBehavior = autoMappingBehavior;
	}

	/**
	 * @since 3.4.0
	 */
	public AutoMappingUnknownColumnBehavior getAutoMappingUnknownColumnBehavior() {
		return autoMappingUnknownColumnBehavior;
	}

	/**
	 * @since 3.4.0
	 */
	public void setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior) {
		this.autoMappingUnknownColumnBehavior = autoMappingUnknownColumnBehavior;
	}

	public boolean isLazyLoadingEnabled() {
		return lazyLoadingEnabled;
	}

	public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
		this.lazyLoadingEnabled = lazyLoadingEnabled;
	}

	public ProxyFactory getProxyFactory() {
		return proxyFactory;
	}

	public void setProxyFactory(ProxyFactory proxyFactory) {
		if (proxyFactory == null) {
			proxyFactory = new JavassistProxyFactory();
		}
		this.proxyFactory = proxyFactory;
	}

	public boolean isAggressiveLazyLoading() {
		return aggressiveLazyLoading;
	}

	public void setAggressiveLazyLoading(boolean aggressiveLazyLoading) {
		this.aggressiveLazyLoading = aggressiveLazyLoading;
	}

	public boolean isMultipleResultSetsEnabled() {
		return multipleResultSetsEnabled;
	}

	public void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled) {
		this.multipleResultSetsEnabled = multipleResultSetsEnabled;
	}

	public Set<String> getLazyLoadTriggerMethods() {
		return lazyLoadTriggerMethods;
	}

	public void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods) {
		this.lazyLoadTriggerMethods = lazyLoadTriggerMethods;
	}

	public boolean isUseGeneratedKeys() {
		return useGeneratedKeys;
	}

	public void setUseGeneratedKeys(boolean useGeneratedKeys) {
		this.useGeneratedKeys = useGeneratedKeys;
	}

	public ExecutorType getDefaultExecutorType() {
		return defaultExecutorType;
	}

	public void setDefaultExecutorType(ExecutorType defaultExecutorType) {
		this.defaultExecutorType = defaultExecutorType;
	}

	public boolean isCacheEnabled() {
		return cacheEnabled;
	}

	public void setCacheEnabled(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
	}

	public Integer getDefaultStatementTimeout() {
		return defaultStatementTimeout;
	}

	public void setDefaultStatementTimeout(Integer defaultStatementTimeout) {
		this.defaultStatementTimeout = defaultStatementTimeout;
	}

	/**
	 * @since 3.3.0
	 */
	public Integer getDefaultFetchSize() {
		return defaultFetchSize;
	}

	/**
	 * @since 3.3.0
	 */
	public void setDefaultFetchSize(Integer defaultFetchSize) {
		this.defaultFetchSize = defaultFetchSize;
	}

	public boolean isUseColumnLabel() {
		return useColumnLabel;
	}

	public void setUseColumnLabel(boolean useColumnLabel) {
		this.useColumnLabel = useColumnLabel;
	}

	public LocalCacheScope getLocalCacheScope() {
		return localCacheScope;
	}

	public void setLocalCacheScope(LocalCacheScope localCacheScope) {
		this.localCacheScope = localCacheScope;
	}

	public JdbcType getJdbcTypeForNull() {
		return jdbcTypeForNull;
	}

	public void setJdbcTypeForNull(JdbcType jdbcTypeForNull) {
		this.jdbcTypeForNull = jdbcTypeForNull;
	}

	public Properties getVariables() {
		return variables;
	}

	public void setVariables(Properties variables) {
		this.variables = variables;
	}

	public TypeHandlerRegistry getTypeHandlerRegistry() {
		return typeHandlerRegistry;
	}

	/**
	 * Set a default {@link TypeHandler} class for {@link Enum}.
	 * A default {@link TypeHandler} is {@link org.apache.ibatis.type.EnumTypeHandler}.
	 *
	 * @param typeHandler a type handler class for {@link Enum}
	 * @since 3.4.5
	 */
	public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
		if (typeHandler != null) {
			getTypeHandlerRegistry().setDefaultEnumTypeHandler(typeHandler);
		}
	}

	public TypeAliasRegistry getTypeAliasRegistry() {
		return typeAliasRegistry;
	}

	/**
	 * @since 3.2.2
	 */
	public MapperRegistry getMapperRegistry() {
		return mapperRegistry;
	}

	public ReflectorFactory getReflectorFactory() {
		return reflectorFactory;
	}

	public void setReflectorFactory(ReflectorFactory reflectorFactory) {
		this.reflectorFactory = reflectorFactory;
	}

	public ObjectFactory getObjectFactory() {
		return objectFactory;
	}

	public void setObjectFactory(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}

	public ObjectWrapperFactory getObjectWrapperFactory() {
		return objectWrapperFactory;
	}

	public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
		this.objectWrapperFactory = objectWrapperFactory;
	}

	/**
	 * @since 3.2.2
	 */
	public List<Interceptor> getInterceptors() {
		return interceptorChain.getInterceptors();
	}

	public LanguageDriverRegistry getLanguageRegistry() {
		return languageRegistry;
	}

	public void setDefaultScriptingLanguage(Class<?> driver) {
		if (driver == null) {
			driver = XMLLanguageDriver.class;
		}
		getLanguageRegistry().setDefaultDriverClass(driver);
	}

	public LanguageDriver getDefaultScriptingLanguageInstance() {
		return languageRegistry.getDefaultDriver();
	}

	/**
	 * @deprecated Use {@link #getDefaultScriptingLanguageInstance()}
	 */
	@Deprecated
	public LanguageDriver getDefaultScriptingLanuageInstance() {
		return getDefaultScriptingLanguageInstance();
	}

	public MetaObject newMetaObject(Object object) {
		return MetaObject.forObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
	}

	public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
		ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
		parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
		return parameterHandler;
	}

	public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler,
	                                            ResultHandler resultHandler, BoundSql boundSql) {
		ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
		resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
		return resultSetHandler;
	}

	public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
		StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
		statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
		return statementHandler;
	}

	public Executor newExecutor(Transaction transaction) {
		return newExecutor(transaction, defaultExecutorType);
	}

	public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
		executorType = executorType == null ? defaultExecutorType : executorType;
		executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
		Executor executor;
		if (ExecutorType.BATCH == executorType) {
			executor = new BatchExecutor(this, transaction);
		} else if (ExecutorType.REUSE == executorType) {
			executor = new ReuseExecutor(this, transaction);
		} else {
			executor = new SimpleExecutor(this, transaction);
		}
		if (cacheEnabled) {
			executor = new CachingExecutor(executor);
		}
		executor = (Executor) interceptorChain.pluginAll(executor);
		return executor;
	}

	public void addKeyGenerator(String id, KeyGenerator keyGenerator) {
		keyGenerators.put(id, keyGenerator);
	}

	public Collection<String> getKeyGeneratorNames() {
		return keyGenerators.keySet();
	}

	public Collection<KeyGenerator> getKeyGenerators() {
		return keyGenerators.values();
	}

	public KeyGenerator getKeyGenerator(String id) {
		return keyGenerators.get(id);
	}

	public boolean hasKeyGenerator(String id) {
		return keyGenerators.containsKey(id);
	}

	public void addCache(Cache cache) {
		caches.put(cache.getId(), cache);
	}

	public Collection<String> getCacheNames() {
		return caches.keySet();
	}

	public Collection<Cache> getCaches() {
		return caches.values();
	}

	public Cache getCache(String id) {
		return caches.get(id);
	}

	public boolean hasCache(String id) {
		return caches.containsKey(id);
	}

	public void addResultMap(ResultMap rm) {
		resultMaps.put(rm.getId(), rm);
		checkLocallyForDiscriminatedNestedResultMaps(rm);
		checkGloballyForDiscriminatedNestedResultMaps(rm);
	}

	public Collection<String> getResultMapNames() {
		return resultMaps.keySet();
	}

	public Collection<ResultMap> getResultMaps() {
		return resultMaps.values();
	}

	public ResultMap getResultMap(String id) {
		return resultMaps.get(id);
	}

	public boolean hasResultMap(String id) {
		return resultMaps.containsKey(id);
	}

	public void addParameterMap(ParameterMap pm) {
		parameterMaps.put(pm.getId(), pm);
	}

	public Collection<String> getParameterMapNames() {
		return parameterMaps.keySet();
	}

	public Collection<ParameterMap> getParameterMaps() {
		return parameterMaps.values();
	}

	public ParameterMap getParameterMap(String id) {
		return parameterMaps.get(id);
	}

	public boolean hasParameterMap(String id) {
		return parameterMaps.containsKey(id);
	}

	public void addMappedStatement(MappedStatement ms) {
		mappedStatements.put(ms.getId(), ms);
	}

	public Collection<String> getMappedStatementNames() {
		buildAllStatements();
		return mappedStatements.keySet();
	}

	public Collection<MappedStatement> getMappedStatements() {
		buildAllStatements();
		return mappedStatements.values();
	}

	public Collection<XMLStatementBuilder> getIncompleteStatements() {
		return incompleteStatements;
	}

	public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {
		incompleteStatements.add(incompleteStatement);
	}

	public Collection<CacheRefResolver> getIncompleteCacheRefs() {
		return incompleteCacheRefs;
	}

	public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {
		incompleteCacheRefs.add(incompleteCacheRef);
	}

	public Collection<ResultMapResolver> getIncompleteResultMaps() {
		return incompleteResultMaps;
	}

	public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {
		incompleteResultMaps.add(resultMapResolver);
	}

	public void addIncompleteMethod(MethodResolver builder) {
		incompleteMethods.add(builder);
	}

	public Collection<MethodResolver> getIncompleteMethods() {
		return incompleteMethods;
	}

	public MappedStatement getMappedStatement(String id) {
		return this.getMappedStatement(id, true);
	}

	public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
		if (validateIncompleteStatements) {
			buildAllStatements();
		}
		return mappedStatements.get(id);
	}

	public Map<String, XNode> getSqlFragments() {
		return sqlFragments;
	}

	public void addInterceptor(Interceptor interceptor) {
		interceptorChain.addInterceptor(interceptor);
	}

	public void addMappers(String packageName, Class<?> superType) {
		mapperRegistry.addMappers(packageName, superType);
	}

	public void addMappers(String packageName) {
		mapperRegistry.addMappers(packageName);
	}

	public <T> void addMapper(Class<T> type) {
		mapperRegistry.addMapper(type);
	}

	public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
		return mapperRegistry.getMapper(type, sqlSession);
	}

	public boolean hasMapper(Class<?> type) {
		return mapperRegistry.hasMapper(type);
	}

	public boolean hasStatement(String statementName) {
		return hasStatement(statementName, true);
	}

	public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
		if (validateIncompleteStatements) {
			buildAllStatements();
		}
		return mappedStatements.containsKey(statementName);
	}

	public void addCacheRef(String namespace, String referencedNamespace) {
		cacheRefMap.put(namespace, referencedNamespace);
	}

	/**
	 * Parses all the unprocessed statement nodes in the cache. It is recommended
	 * to call this method once all the mappers are added as it provides fail-fast
	 * statement validation.
	 * <p>
	 * 构造 语句啊
	 * 将未完成的 那些 incomplete 全部执行以便
	 */
	private void buildAllStatements() {
		if (!incompleteResultMaps.isEmpty()) {
			synchronized (incompleteResultMaps) {
				// This always throws a BuilderException.
				incompleteResultMaps.iterator().next().resolve();
			}
		}
		if (!incompleteCacheRefs.isEmpty()) {
			synchronized (incompleteCacheRefs) {
				// This always throws a BuilderException.
				incompleteCacheRefs.iterator().next().resolveCacheRef();
			}
		}
		if (!incompleteStatements.isEmpty()) {
			synchronized (incompleteStatements) {
				// This always throws a BuilderException.
				incompleteStatements.iterator().next().parseStatementNode();
			}
		}
		if (!incompleteMethods.isEmpty()) {
			synchronized (incompleteMethods) {
				// This always throws a BuilderException.
				incompleteMethods.iterator().next().resolve();
			}
		}
	}

	/**
	 * Extracts namespace from fully qualified statement id.
	 * <p>
	 * 通过 全部的String 抽取出来最后的 name
	 *
	 * @param statementId
	 * @return namespace or null when id does not contain period.
	 */
	protected String extractNamespace(String statementId) {
		int lastPeriod = statementId.lastIndexOf('.');
		return lastPeriod > 0 ? statementId.substring(0, lastPeriod) : null;
	}

	/**
	 * Slow but a one time cost. A better solution is welcome.
	 * 检查 本地结果集是否 嵌套
	 *
	 * @param rm
	 */
	private void checkGloballyForDiscriminatedNestedResultMaps(ResultMap rm) {
		if (rm.hasNestedResultMaps()) {
			for (Map.Entry<String, ResultMap> entry : resultMaps.entrySet()) {
				Object value = entry.getValue();
				if (value instanceof ResultMap) {
					ResultMap entryResultMap = (ResultMap) value;
					if (!entryResultMap.hasNestedResultMaps() && entryResultMap.getDiscriminator() != null) {
						Collection<String> discriminatedResultMapNames = entryResultMap.getDiscriminator().getDiscriminatorMap().values();
						if (discriminatedResultMapNames.contains(rm.getId())) {
							entryResultMap.forceNestedResultMaps();
						}
					}
				}
			}
		}
	}

	/**
	 * Slow but a one time cost. A better solution is welcome.
	 * <p>
	 * 检查 本地结果集是否 嵌套
	 *
	 * @param rm
	 */
	private void checkLocallyForDiscriminatedNestedResultMaps(ResultMap rm) {
		if (!rm.hasNestedResultMaps() && rm.getDiscriminator() != null) {
			for (Map.Entry<String, String> entry : rm.getDiscriminator().getDiscriminatorMap().entrySet()) {
				String discriminatedResultMapName = entry.getValue();
				if (hasResultMap(discriminatedResultMapName)) {
					ResultMap discriminatedResultMap = resultMaps.get(discriminatedResultMapName);
					if (discriminatedResultMap.hasNestedResultMaps()) {
						rm.forceNestedResultMaps();
						break;
					}
				}
			}
		}
	}

	/**
	 * 一个有严格的map 用来存储信息吧
	 * key是String  value 就是相应的对象
	 * 不能put 重复的key
	 *
	 * @param <V>
	 */
	private static class StrictMap<V> extends HashMap<String, V> {

		private static final long serialVersionUID = -4950446264854982944L;

		/**
		 * map 的名称
		 */
		private final String name;

		public StrictMap(String name, int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor);
			this.name = name;
		}

		public StrictMap(String name, int initialCapacity) {
			super(initialCapacity);
			this.name = name;
		}

		public StrictMap(String name) {
			super();
			this.name = name;
		}

		public StrictMap(String name, Map<String, ? extends V> m) {
			super(m);
			this.name = name;
		}

		@SuppressWarnings("unchecked")
		@Override
		public V put(String key, V value) {
			if (containsKey(key)) {
				throw new IllegalArgumentException(name + " already contains value for " + key);
			}
			if (key.contains(".")) {
				final String shortKey = getShortName(key);
				if (super.get(shortKey) == null) {
					super.put(shortKey, value);
				} else {
					super.put(shortKey, (V) new Ambiguity(shortKey));
				}
			}
			return super.put(key, value);
		}

		@Override
		public V get(Object key) {
			V value = super.get(key);
			if (value == null) {
				throw new IllegalArgumentException(name + " does not contain value for " + key);
			}
			if (value instanceof Ambiguity) {
				throw new IllegalArgumentException(((Ambiguity) value).getSubject() + " is ambiguous in " + name
						+ " (try using the full name including the namespace, or rename one of the entries)");
			}
			return value;
		}

		/**
		 * 得到短name
		 * abc.def.shortName = shortName
		 *
		 * @param key
		 * @return
		 */
		private String getShortName(String key) {
			// java 转义 正则转义
			final String[] keyParts = key.split("\\.");
			return keyParts[keyParts.length - 1];
		}

		/**
		 * 有歧义的 对应的类
		 * 有歧义的主题
		 */
		private static class Ambiguity {
			final private String subject;

			Ambiguity(String subject) {
				this.subject = subject;
			}

			String getSubject() {
				return subject;
			}
		}
	}
}

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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 * 解析XML  获取配置文件的  Builder 继承BaseBuilder 获得一系列的工具方法
 * XML解析的是否 关于配置相关的操作 都有一步就是 先获得整体的Properties 然后 在根据子节点再去填充属性
 * 这就是很多XML 中公共配置,地下替换的操作把    就是一堆工厂模式
 * 这个类很重要啊,Mybatis配置必须通过这个类
 * 我们可以通过这个类来学习一些参数,以及一个Properties的使用场景
 */
public class XMLConfigBuilder extends BaseBuilder {

	/**
	 * 标识符 仅一次
	 */
	private boolean parsed;
	/**
	 * 解析XML离不开XPath
	 */
	private final XPathParser parser;
	private String environment;
	private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();


	public XMLConfigBuilder(Reader reader) {

		// 直接流传入
		this(reader, null, null);
	}

	public XMLConfigBuilder(Reader reader, String environment) {
		// 流配合String
		this(reader, environment, null);
	}

	public XMLConfigBuilder(Reader reader, String environment, Properties props) {
		// 这个构造函数应该是最常用的 ,创建一个XPath解析器 和 XMLMapper实体发现器
		this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
	}

	public XMLConfigBuilder(InputStream inputStream) {
		this(inputStream, null, null);
	}

	public XMLConfigBuilder(InputStream inputStream, String environment) {
		this(inputStream, environment, null);
	}

	public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
		this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
	}

	private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
		super(new Configuration());
		ErrorContext.instance().resource("SQL Mapper Configuration");
		this.configuration.setVariables(props);
		this.parsed = false;
		this.environment = environment;
		this.parser = parser;
	}

	public Configuration parse() {
		// 只能解析一次
		if (parsed) {
			throw new BuilderException("Each XMLConfigBuilder can only be used once.");
		}
		parsed = true;
		// 解析节点,知己使用XPath 超级舒服
		parseConfiguration(parser.evalNode("/configuration"));
		return configuration;
	}

	/**
	 * 学习下 这个方法的编写,尽可能的抽取方法,让逻辑清晰,然后方法也好测试
	 * @param root
	 */
	private void parseConfiguration(XNode root) {
		// 对应的节点就开始了
		try {
			//issue #117 read properties first   这个最先调用 后面依赖 Properties
			propertiesElement(root.evalNode("properties"));
			Properties settings = settingsAsProperties(root.evalNode("settings"));
			loadCustomVfs(settings);
			typeAliasesElement(root.evalNode("typeAliases"));
			pluginElement(root.evalNode("plugins"));
			objectFactoryElement(root.evalNode("objectFactory"));
			objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
			reflectorFactoryElement(root.evalNode("reflectorFactory"));
			// 模式设置
			settingsElement(settings);
			// read it after objectFactory and objectWrapperFactory issue #631
			environmentsElement(root.evalNode("environments"));
			databaseIdProviderElement(root.evalNode("databaseIdProvider"));
			typeHandlerElement(root.evalNode("typeHandlers"));
			mapperElement(root.evalNode("mappers"));
		} catch (Exception e) {
			throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
		}
	}

	/**
	 * 处理settings节点
	 * @param context
	 * @return
	 */
	private Properties settingsAsProperties(XNode context) {
		if (context == null) {
			return new Properties();
		}
		Properties props = context.getChildrenAsProperties();
		// Check that all settings are known to the configuration class
		MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
		for (Object key : props.keySet()) {
			// 将settings 节点中转化成Properties
			if (!metaConfig.hasSetter(String.valueOf(key))) {
				throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
			}
		}
		return props;
	}

	/**
	 *  加载配置信息到容器   VFS有没有印象,在看一遍
	 * @param props
	 * @throws ClassNotFoundException
	 */
	private void loadCustomVfs(Properties props) throws ClassNotFoundException {
		String value = props.getProperty("vfsImpl");
		if (value != null) {
			String[] clazzes = value.split(",");
			for (String clazz : clazzes) {
				if (!clazz.isEmpty()) {
					@SuppressWarnings("unchecked")
					Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources.classForName(clazz);
					configuration.setVfsImpl(vfsImpl);
				}
			}
		}
	}

	/**
	 * 处理typeAliases
	 * @param parent
	 */
	private void typeAliasesElement(XNode parent) {
		if (parent != null) {
			// 循环 然后然后开始想配置中进行别名注册追加
			for (XNode child : parent.getChildren()) {
				if ("package".equals(child.getName())) {
					String typeAliasPackage = child.getStringAttribute("name");
					configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
				} else {
					String alias = child.getStringAttribute("alias");
					String type = child.getStringAttribute("type");
					try {
						Class<?> clazz = Resources.classForName(type);
						if (alias == null) {
							typeAliasRegistry.registerAlias(clazz);
						} else {
							typeAliasRegistry.registerAlias(alias, clazz);
						}
					} catch (ClassNotFoundException e) {
						throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
					}
				}
			}
		}
	}

	/**
	 * plugins 插件解析 ,就是Interceptor啊
	 * @param parent
	 * @throws Exception
	 */
	private void pluginElement(XNode parent) throws Exception {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				String interceptor = child.getStringAttribute("interceptor");
				Properties properties = child.getChildrenAsProperties();
				// 直接类名反射出来 然后添加到配置中
				Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
				interceptorInstance.setProperties(properties);
				configuration.addInterceptor(interceptorInstance);
			}
		}
	}

	/**
	 * objectFactory 节点解析
	 * @param context
	 * @throws Exception
	 */
	private void objectFactoryElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			Properties properties = context.getChildrenAsProperties();
			ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
			factory.setProperties(properties);
			configuration.setObjectFactory(factory);
		}
	}

	/**
	 * 解析 objectWrapperFactory
	 * @param context
	 * @throws Exception
	 */
	private void objectWrapperFactoryElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
			configuration.setObjectWrapperFactory(factory);
		}
	}

	/**
	 * reflectorFactory 反射工程
	 * @param context
	 * @throws Exception
	 */
	private void reflectorFactoryElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
			configuration.setReflectorFactory(factory);
		}
	}

	/**
	 * 处理properties节点
	 * @param context
	 * @throws Exception
	 */
	private void propertiesElement(XNode context) throws Exception {
		if (context != null) {
			// 获取子节点转化成Properties
			Properties defaults = context.getChildrenAsProperties();
			// 属性
			String resource = context.getStringAttribute("resource");
			String url = context.getStringAttribute("url");
			if (resource != null && url != null) {
				throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
			}
			if (resource != null) {
				defaults.putAll(Resources.getResourceAsProperties(resource));
			} else if (url != null) {
				defaults.putAll(Resources.getUrlAsProperties(url));
			}
			Properties vars = configuration.getVariables();
			if (vars != null) {
				defaults.putAll(vars);
			}
			parser.setVariables(defaults);
			// 1. properties 节点内的所有子节点
			// 2. 子节点 resource 的 value 资源加载的properties
			// 3. 子节点 url 的 value 资源加载的properties
			// 4. configuration 自带的一些properties   就是一个父子 XML 明白吧
			configuration.setVariables(defaults);
		}
	}

	/**
	 * 开始了 默认值走起来
	 * @param props
	 * @throws Exception
	 */
	private void settingsElement(Properties props) throws Exception {
		configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
		configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
		configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
		configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
		configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
		configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
		configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
		configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
		configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
		configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
		configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
		configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
		configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
		configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
		configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
		configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
		configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
		configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
		configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
		@SuppressWarnings("unchecked")
		Class<? extends TypeHandler> typeHandler = (Class<? extends TypeHandler>) resolveClass(props.getProperty("defaultEnumTypeHandler"));
		configuration.setDefaultEnumTypeHandler(typeHandler);
		configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
		configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
		configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
		configuration.setLogPrefix(props.getProperty("logPrefix"));
		@SuppressWarnings("unchecked")
		Class<? extends Log> logImpl = (Class<? extends Log>) resolveClass(props.getProperty("logImpl"));
		configuration.setLogImpl(logImpl);
		configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
	}

	/**
	 * environments 解析
	 * @param context
	 * @throws Exception
	 */
	private void environmentsElement(XNode context) throws Exception {
		if (context != null) {
			if (environment == null) {
				environment = context.getStringAttribute("default");
			}
			for (XNode child : context.getChildren()) {
				String id = child.getStringAttribute("id");
				if (isSpecifiedEnvironment(id)) {
					TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
					DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
					DataSource dataSource = dsFactory.getDataSource();
					Environment.Builder environmentBuilder = new Environment.Builder(id)
							.transactionFactory(txFactory)
							.dataSource(dataSource);
					configuration.setEnvironment(environmentBuilder.build());
				}
			}
		}
	}

	/**
	 * databaseIdProvider 数据库提供
	 *
	 * @param context
	 * @throws Exception
	 */
	private void databaseIdProviderElement(XNode context) throws Exception {
		DatabaseIdProvider databaseIdProvider = null;
		if (context != null) {
			String type = context.getStringAttribute("type");
			// awful patch to keep backward compatibility
			if ("VENDOR".equals(type)) {
				type = "DB_VENDOR";
			}
			Properties properties = context.getChildrenAsProperties();
			databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
			databaseIdProvider.setProperties(properties);
		}
		Environment environment = configuration.getEnvironment();
		if (environment != null && databaseIdProvider != null) {
			String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
			configuration.setDatabaseId(databaseId);
		}
	}

	private TransactionFactory transactionManagerElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			Properties props = context.getChildrenAsProperties();
			TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
			factory.setProperties(props);
			return factory;
		}
		throw new BuilderException("Environment declaration requires a TransactionFactory.");
	}

	private DataSourceFactory dataSourceElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			Properties props = context.getChildrenAsProperties();
			DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
			factory.setProperties(props);
			return factory;
		}
		throw new BuilderException("Environment declaration requires a DataSourceFactory.");
	}

	/**
	 * typeHandlers 进行处理啊  自定义类型处理
	 * @param parent
	 * @throws Exception
	 */
	private void typeHandlerElement(XNode parent) throws Exception {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				if ("package".equals(child.getName())) {
					String typeHandlerPackage = child.getStringAttribute("name");
					typeHandlerRegistry.register(typeHandlerPackage);
				} else {
					String javaTypeName = child.getStringAttribute("javaType");
					String jdbcTypeName = child.getStringAttribute("jdbcType");
					String handlerTypeName = child.getStringAttribute("handler");
					Class<?> javaTypeClass = resolveClass(javaTypeName);
					JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
					Class<?> typeHandlerClass = resolveClass(handlerTypeName);
					if (javaTypeClass != null) {
						if (jdbcType == null) {
							typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
						} else {
							typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
						}
					} else {
						typeHandlerRegistry.register(typeHandlerClass);
					}
				}
			}
		}
	}

	/**
	 * mappers 解析
	 * @param parent
	 * @throws Exception
	 */
	private void mapperElement(XNode parent) throws Exception {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				if ("package".equals(child.getName())) {
					String mapperPackage = child.getStringAttribute("name");
					configuration.addMappers(mapperPackage);
				} else {
					String resource = child.getStringAttribute("resource");
					String url = child.getStringAttribute("url");
					String mapperClass = child.getStringAttribute("class");
					if (resource != null && url == null && mapperClass == null) {
						ErrorContext.instance().resource(resource);
						InputStream inputStream = Resources.getResourceAsStream(resource);
						XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
						mapperParser.parse();
					} else if (resource == null && url != null && mapperClass == null) {
						ErrorContext.instance().resource(url);
						InputStream inputStream = Resources.getUrlAsStream(url);
						// 这里面注解构造出来XMLMapperBuilder
						XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
						mapperParser.parse();
					} else if (resource == null && url == null && mapperClass != null) {
						Class<?> mapperInterface = Resources.classForName(mapperClass);
						// mapper class 添加到配置类中
						configuration.addMapper(mapperInterface);
					} else {
						throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
					}
				}
			}
		}
	}

	private boolean isSpecifiedEnvironment(String id) {
		if (environment == null) {
			throw new BuilderException("No environment specified.");
		} else if (id == null) {
			throw new BuilderException("Environment requires an id attribute.");
		} else if (environment.equals(id)) {
			return true;
		}
		return false;
	}

}

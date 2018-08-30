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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 * 同样继承BaseBuilder
 * 暂时是被XMLConfigBuilder 来调用构造的
 * Mapper 还是一个XML 因此还需要 XPathParse 以及某些操作
 * 不过XMLMapper 唯一一个特殊指出就是需要存储 sql
 */
public class XMLMapperBuilder extends BaseBuilder {

	/**
	 * XPath 解析器
	 */
	private final XPathParser parser;
	/**
	 * 分工十分明确,通过MapperBuilderAssistant 来进行辅助解析
	 */
	private final MapperBuilderAssistant builderAssistant;
	private final Map<String, XNode> sqlFragments;
	/**
	 * 资源路径
	 */
	private final String resource;

	@Deprecated
	public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
		this(reader, configuration, resource, sqlFragments);
		this.builderAssistant.setCurrentNamespace(namespace);
	}

	@Deprecated
	public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
		this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()),
				configuration, resource, sqlFragments);
	}

	public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
		this(inputStream, configuration, resource, sqlFragments);
		this.builderAssistant.setCurrentNamespace(namespace);
	}

	public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
		this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()),
				configuration, resource, sqlFragments);
	}

	/**
	 * 最重要的 构造方法
	 * @param parser
	 * @param configuration
	 * @param resource
	 * @param sqlFragments
	 */
	private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
		super(configuration);
		this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
		this.parser = parser;
		this.sqlFragments = sqlFragments;
		this.resource = resource;
	}

	/**
	 * 第一步解析啊
	 * 学习下方法怎么写的
	 * 这个方法 应该保证单线程调用
	 */
	public void parse() {
		// XMLMapper 已经被加载 通过 Configuration来判断
		if (!configuration.isResourceLoaded(resource)) {
			// 解析mapper
			configurationElement(parser.evalNode("/mapper"));
			// 添加到configuration中 防止多次加载
			configuration.addLoadedResource(resource);
			bindMapperForNamespace();
		}
		// 配置解析完毕 需要 pending
		// 讲结果集 缓存关联 语句 都进行解析占用

		parsePendingResultMaps();
		parsePendingCacheRefs();
		parsePendingStatements();
	}

	public XNode getSqlFragment(String refid) {
		return sqlFragments.get(refid);
	}

	/**
	 * 配置元素
	 * 主要是 配置 mapper
	 * @param context
	 */
	private void configurationElement(XNode context) {
		try {
			// 拿到namespace
			String namespace = context.getStringAttribute("namespace");
			if (namespace == null || namespace.equals("")) {
				throw new BuilderException("Mapper's namespace cannot be empty");
			}
			// void 方法给内部实例赋值
			builderAssistant.setCurrentNamespace(namespace);
			cacheRefElement(context.evalNode("cache-ref"));
			cacheElement(context.evalNode("cache"));
			parameterMapElement(context.evalNodes("/mapper/parameterMap"));
			resultMapElements(context.evalNodes("/mapper/resultMap"));
			sqlElement(context.evalNodes("/mapper/sql"));
			buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
		} catch (Exception e) {
			throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
		}
	}

	/**
	 * select|insert|update|delete    这些XPath 的操作 进行构建并且存储啊
	 * @param list
	 */
	private void buildStatementFromContext(List<XNode> list) {
		if (configuration.getDatabaseId() != null) {
			buildStatementFromContext(list, configuration.getDatabaseId());
		}
		buildStatementFromContext(list, null);
	}

	/**
	 * sql 节点 中的 语句节点 进行解析
	 * @param list
	 * @param requiredDatabaseId
	 */
	private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
		for (XNode context : list) {
			// 直接构造XMLStatementBuilder 然后进行代码构造
			final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
			try {
				statementParser.parseStatementNode();
			} catch (IncompleteElementException e) {
				configuration.addIncompleteStatement(statementParser);
			}
		}
	}

	/**
	 *  解析后填充ResultMaps啊
	 */
	private void parsePendingResultMaps() {
		Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
		synchronized (incompleteResultMaps) {
			// 为占用的 都进行 resolve()
			Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
			while (iter.hasNext()) {
				try {
					iter.next().resolve();
					iter.remove();
				} catch (IncompleteElementException e) {
					// ResultMap is still missing a resource...
				}
			}
		}
	}

	private void parsePendingCacheRefs() {
		Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
		synchronized (incompleteCacheRefs) {
			// 为占用的 都进行 resolve()
			Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
			while (iter.hasNext()) {
				try {
					iter.next().resolveCacheRef();
					iter.remove();
				} catch (IncompleteElementException e) {
					// Cache ref is still missing a resource...
				}
			}
		}
	}

	private void parsePendingStatements() {
		Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
		synchronized (incompleteStatements) {
			// 为占用的 都进行 resolve()
			Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
			while (iter.hasNext()) {
				try {
					iter.next().parseStatementNode();
					iter.remove();
				} catch (IncompleteElementException e) {
					// Statement is still missing a resource...
				}
			}
		}
	}

	/**
	 * 解析 cache-ref
	 * @param context
	 */
	private void cacheRefElement(XNode context) {
		if (context != null) {
			// 缓存的 namespace
			configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
			// 对象创建 namespace
			CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
			try {
				// 开启缓存
				cacheRefResolver.resolveCacheRef();
			} catch (IncompleteElementException e) {
				// 一场了就要删除,一定要确保没有问题
				configuration.addIncompleteCacheRef(cacheRefResolver);
			}
		}
	}

	/**
	 * cache 解析
	 * @param context
	 * @throws Exception
	 */
	private void cacheElement(XNode context) throws Exception {
		if (context != null) {
			// mybatis 缓存是装饰者模式   所以 可以进行功能嵌套
			// 缓存类型 缓存的实现类
			String type = context.getStringAttribute("type", "PERPETUAL");
			Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
			// 缓存淘汰 缓存的实现类
			String eviction = context.getStringAttribute("eviction", "LRU");
			Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
			// 刷新 间隔
			Long flushInterval = context.getLongAttribute("flushInterval");
			Integer size = context.getIntAttribute("size");
			// 制只读
			boolean readWrite = !context.getBooleanAttribute("readOnly", false);
			// 阻塞
			boolean blocking = context.getBooleanAttribute("blocking", false);
			Properties props = context.getChildrenAsProperties();
			// 通过 builderAssistant 来构造一个协助缓存
			builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
		}
	}

	/**
	 * 解析 /mapper/parameterMap    XPath
	 * 解析玩不生成ParameterMapping 存进去
	 * set 到辅助类中
	 * @param list
	 * @throws Exception
	 */
	private void parameterMapElement(List<XNode> list) throws Exception {
		// mappers 中的多个 parameterMap
		for (XNode parameterMapNode : list) {
			String id = parameterMapNode.getStringAttribute("id");
			String type = parameterMapNode.getStringAttribute("type");
			// 通过 tyep 拿到类
			Class<?> parameterClass = resolveClass(type);
			List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
			List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
			for (XNode parameterNode : parameterNodes) {
				String property = parameterNode.getStringAttribute("property");
				String javaType = parameterNode.getStringAttribute("javaType");
				String jdbcType = parameterNode.getStringAttribute("jdbcType");
				String resultMap = parameterNode.getStringAttribute("resultMap");
				String mode = parameterNode.getStringAttribute("mode");
				String typeHandler = parameterNode.getStringAttribute("typeHandler");
				Integer numericScale = parameterNode.getIntAttribute("numericScale");
				ParameterMode modeEnum = resolveParameterMode(mode);
				Class<?> javaTypeClass = resolveClass(javaType);
				JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
				@SuppressWarnings("unchecked")
				Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
				ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
				parameterMappings.add(parameterMapping);
			}
			// set 到 辅助 类中
			// id 对应的类和 影射的类
			builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
		}
	}

	/**
	 * 解析 /mapper/resultMap
	 * 多个
	 * @param list
	 * @throws Exception
	 */
	private void resultMapElements(List<XNode> list) throws Exception {
		for (XNode resultMapNode : list) {
			try {
				resultMapElement(resultMapNode);
			} catch (IncompleteElementException e) {
				// ignore, it will be retried
			}
		}
	}

	/**
	 * 方法细分了
	 * @param resultMapNode
	 * @return
	 * @throws Exception
	 */
	private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
		return resultMapElement(resultMapNode, Collections.<ResultMapping>emptyList());
	}

	/**
	 * 解决 返回值元素的
	 * 需要传入一个集合的
	 * @param resultMapNode
	 * @param additionalResultMappings
	 * @return
	 * @throws Exception
	 */
	private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings) throws Exception {
		ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
		String id = resultMapNode.getStringAttribute("id",
				resultMapNode.getValueBasedIdentifier());
		// 这个默认值有点厉害啊
		String type = resultMapNode.getStringAttribute("type",
				resultMapNode.getStringAttribute("ofType",
						resultMapNode.getStringAttribute("resultType",
								resultMapNode.getStringAttribute("javaType"))));
		String extend = resultMapNode.getStringAttribute("extends");
		Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
		Class<?> typeClass = resolveClass(type);
		Discriminator discriminator = null;
		List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
		resultMappings.addAll(additionalResultMappings);
		List<XNode> resultChildren = resultMapNode.getChildren();
		for (XNode resultChild : resultChildren) {
			if ("constructor".equals(resultChild.getName())) {
				processConstructorElement(resultChild, typeClass, resultMappings);
			} else if ("discriminator".equals(resultChild.getName())) {
				discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
			} else {
				List<ResultFlag> flags = new ArrayList<ResultFlag>();
				if ("id".equals(resultChild.getName())) {
					flags.add(ResultFlag.ID);
				}
				resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
			}
		}
		// 构造成ResultMapResolver   然后 掉哟个resolve方法  有修改了builderAssistant对象
		ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
		try {
			return resultMapResolver.resolve();
		} catch (IncompleteElementException e) {
			configuration.addIncompleteResultMap(resultMapResolver);
			throw e;
		}
	}

	private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
		List<XNode> argChildren = resultChild.getChildren();
		for (XNode argChild : argChildren) {
			List<ResultFlag> flags = new ArrayList<ResultFlag>();
			flags.add(ResultFlag.CONSTRUCTOR);
			if ("idArg".equals(argChild.getName())) {
				flags.add(ResultFlag.ID);
			}
			resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
		}
	}

	private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
		String column = context.getStringAttribute("column");
		String javaType = context.getStringAttribute("javaType");
		String jdbcType = context.getStringAttribute("jdbcType");
		String typeHandler = context.getStringAttribute("typeHandler");
		Class<?> javaTypeClass = resolveClass(javaType);
		@SuppressWarnings("unchecked")
		Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
		JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
		Map<String, String> discriminatorMap = new HashMap<String, String>();
		for (XNode caseChild : context.getChildren()) {
			String value = caseChild.getStringAttribute("value");
			String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings));
			discriminatorMap.put(value, resultMap);
		}
		return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
	}

	/**
	 * 解析 /mapper/sql
	 * @param list
	 * @throws Exception
	 */
	private void sqlElement(List<XNode> list) throws Exception {
		if (configuration.getDatabaseId() != null) {
			sqlElement(list, configuration.getDatabaseId());
		}
		sqlElement(list, null);
	}

	/**
	 * 解析 Sql
	 * @param list
	 * @param requiredDatabaseId
	 * @throws Exception
	 */
	private void sqlElement(List<XNode> list, String requiredDatabaseId) throws Exception {
		for (XNode context : list) {
			String databaseId = context.getStringAttribute("databaseId");
			String id = context.getStringAttribute("id");
			id = builderAssistant.applyCurrentNamespace(id, false);
			if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
				// 添加到 sqlFragments 中
				sqlFragments.put(id, context);
			}
		}
	}

	private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
		if (requiredDatabaseId != null) {
			if (!requiredDatabaseId.equals(databaseId)) {
				return false;
			}
		} else {
			if (databaseId != null) {
				return false;
			}
			// skip this fragment if there is a previous one with a not null databaseId
			if (this.sqlFragments.containsKey(id)) {
				XNode context = this.sqlFragments.get(id);
				if (context.getStringAttribute("databaseId") != null) {
					return false;
				}
			}
		}
		return true;
	}

	private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
		String property;
		if (flags.contains(ResultFlag.CONSTRUCTOR)) {
			property = context.getStringAttribute("name");
		} else {
			property = context.getStringAttribute("property");
		}
		String column = context.getStringAttribute("column");
		String javaType = context.getStringAttribute("javaType");
		String jdbcType = context.getStringAttribute("jdbcType");
		String nestedSelect = context.getStringAttribute("select");
		String nestedResultMap = context.getStringAttribute("resultMap",
				processNestedResultMappings(context, Collections.<ResultMapping>emptyList()));
		String notNullColumn = context.getStringAttribute("notNullColumn");
		String columnPrefix = context.getStringAttribute("columnPrefix");
		String typeHandler = context.getStringAttribute("typeHandler");
		String resultSet = context.getStringAttribute("resultSet");
		String foreignColumn = context.getStringAttribute("foreignColumn");
		boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
		Class<?> javaTypeClass = resolveClass(javaType);
		@SuppressWarnings("unchecked")
		Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
		JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
		return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
	}

	private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings) throws Exception {
		if ("association".equals(context.getName())
				|| "collection".equals(context.getName())
				|| "case".equals(context.getName())) {
			if (context.getStringAttribute("select") == null) {
				ResultMap resultMap = resultMapElement(context, resultMappings);
				return resultMap.getId();
			}
		}
		return null;
	}

	/**
	 * 连理mapper 文件 和 接口的映射
	 */
	private void bindMapperForNamespace() {
		String namespace = builderAssistant.getCurrentNamespace();
		if (namespace != null) {
			Class<?> boundType = null;
			try {
				boundType = Resources.classForName(namespace);
			} catch (ClassNotFoundException e) {
				//ignore, bound type is not required
			}
			if (boundType != null) {
				if (!configuration.hasMapper(boundType)) {
					// Spring may not know the real resource name so we set a flag
					// to prevent loading again this resource from the mapper interface
					// look at MapperAnnotationBuilder#loadXmlResource
					configuration.addLoadedResource("namespace:" + namespace);
					configuration.addMapper(boundType);
				}
			}
		}
	}

}

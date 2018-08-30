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

import java.util.List;
import java.util.Locale;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 * XMLStatementBuilder Mapper 中的 sql 进行构建
 * 整体操作挺麻烦
 */
public class XMLStatementBuilder extends BaseBuilder {

	/**
	 * Mapper 构造辅助类
	 */
	private final MapperBuilderAssistant builderAssistant;
	private final XNode context;
	private final String requiredDatabaseId;

	public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context) {
		this(configuration, builderAssistant, context, null);
	}

	/**
	 * 核心的构造方法
	 * @param configuration
	 * @param builderAssistant
	 * @param context
	 * @param databaseId
	 */
	public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context, String databaseId) {
		super(configuration);
		this.builderAssistant = builderAssistant;
		this.context = context;
		this.requiredDatabaseId = databaseId;
	}

	/**
	 * 主要调用的 解析语句
	 */
	public void parseStatementNode() {
		String id = context.getStringAttribute("id");
		String databaseId = context.getStringAttribute("databaseId");

		if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
			return;
		}

		Integer fetchSize = context.getIntAttribute("fetchSize");
		Integer timeout = context.getIntAttribute("timeout");
		String parameterMap = context.getStringAttribute("parameterMap");
		String parameterType = context.getStringAttribute("parameterType");
		Class<?> parameterTypeClass = resolveClass(parameterType);
		String resultMap = context.getStringAttribute("resultMap");
		String resultType = context.getStringAttribute("resultType");
		String lang = context.getStringAttribute("lang");
		LanguageDriver langDriver = getLanguageDriver(lang);

		// 通过xml 中的属性 然后去找到一些 对应的java对象
		Class<?> resultTypeClass = resolveClass(resultType);
		String resultSetType = context.getStringAttribute("resultSetType");
		// 语句类型   statement    prepared     callable
		StatementType statementType = StatementType.valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
		// 结果集类型
		ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);

		String nodeName = context.getNode().getNodeName();
		// 什么语句 insert update delete select
		SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
		boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
		// 查询使用缓存    不查询刷新缓存
		boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
		boolean useCache = context.getBooleanAttribute("useCache", isSelect);
		boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);

		// Include Fragments before parsing
		XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
		includeParser.applyIncludes(context.getNode());

		// Parse selectKey after includes and remove them.
		// 同样 selectKey 帮助我们生成key的
		processSelectKeyNodes(id, parameterTypeClass, langDriver);

		// Parse the SQL (pre: <selectKey> and <include> were parsed and removed)
		SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass);
		String resultSets = context.getStringAttribute("resultSets");
		String keyProperty = context.getStringAttribute("keyProperty");
		String keyColumn = context.getStringAttribute("keyColumn");
		KeyGenerator keyGenerator;
		String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
		keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
		// 是否生成主键
		if (configuration.hasKeyGenerator(keyStatementId)) {
			keyGenerator = configuration.getKeyGenerator(keyStatementId);
		} else {
			keyGenerator = context.getBooleanAttribute("useGeneratedKeys",
					configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType))
					? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
		}

		// 通过 builderAssistant 来添加mapperStatement
		builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
				fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
				resultSetTypeEnum, flushCache, useCache, resultOrdered,
				keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets);
	}

	/**
	 * 解析 SelectKey 操作
	 * @param id
	 * @param parameterTypeClass
	 * @param langDriver
	 */
	private void processSelectKeyNodes(String id, Class<?> parameterTypeClass, LanguageDriver langDriver) {
		List<XNode> selectKeyNodes = context.evalNodes("selectKey");
		if (configuration.getDatabaseId() != null) {
			parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, configuration.getDatabaseId());
		}
		parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, null);
		removeSelectKeyNodes(selectKeyNodes);
	}

	/**
	 * 解析 节点
	 * @param parentId
	 * @param list
	 * @param parameterTypeClass
	 * @param langDriver
	 * @param skRequiredDatabaseId
	 */
	private void parseSelectKeyNodes(String parentId, List<XNode> list, Class<?> parameterTypeClass, LanguageDriver langDriver, String skRequiredDatabaseId) {
		for (XNode nodeToHandle : list) {
			String id = parentId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
			String databaseId = nodeToHandle.getStringAttribute("databaseId");
			if (databaseIdMatchesCurrent(id, databaseId, skRequiredDatabaseId)) {
				// 判断后解析
				parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, langDriver, databaseId);
			}
		}
	}

	private void parseSelectKeyNode(String id, XNode nodeToHandle, Class<?> parameterTypeClass, LanguageDriver langDriver, String databaseId) {
		String resultType = nodeToHandle.getStringAttribute("resultType");
		// 获取 resultType 类型
		Class<?> resultTypeClass = resolveClass(resultType);
		StatementType statementType = StatementType.valueOf(nodeToHandle.getStringAttribute("statementType", StatementType.PREPARED.toString()));
		String keyProperty = nodeToHandle.getStringAttribute("keyProperty");
		String keyColumn = nodeToHandle.getStringAttribute("keyColumn");
		boolean executeBefore = "BEFORE".equals(nodeToHandle.getStringAttribute("order", "AFTER"));

		//defaults
		boolean useCache = false;
		boolean resultOrdered = false;
		KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
		Integer fetchSize = null;
		Integer timeout = null;
		boolean flushCache = false;
		String parameterMap = null;
		String resultMap = null;
		ResultSetType resultSetTypeEnum = null;

		// 通过langDriver 来构建SqlSource
		SqlSource sqlSource = langDriver.createSqlSource(configuration, nodeToHandle, parameterTypeClass);
		// 然后通过 select
		SqlCommandType sqlCommandType = SqlCommandType.SELECT;

		builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
				fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
				resultSetTypeEnum, flushCache, useCache, resultOrdered,
				keyGenerator, keyProperty, keyColumn, databaseId, langDriver, null);

		id = builderAssistant.applyCurrentNamespace(id, false);

		MappedStatement keyStatement = configuration.getMappedStatement(id, false);
		configuration.addKeyGenerator(id, new SelectKeyGenerator(keyStatement, executeBefore));
	}

	/**
	 * 解析完毕 删除 SelectKey 节点
	 * @param selectKeyNodes
	 */
	private void removeSelectKeyNodes(List<XNode> selectKeyNodes) {
		for (XNode nodeToHandle : selectKeyNodes) {
			nodeToHandle.getParent().getNode().removeChild(nodeToHandle.getNode());
		}
	}

	/**
	 * 判断 datebaseId 是否同一个
	 * @param id
	 * @param databaseId
	 * @param requiredDatabaseId
	 * @return
	 */
	private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
		if (requiredDatabaseId != null) {
			if (!requiredDatabaseId.equals(databaseId)) {
				return false;
			}
		} else {
			if (databaseId != null) {
				return false;
			}
			// skip this statement if there is a previous one with a not null databaseId
			id = builderAssistant.applyCurrentNamespace(id, false);
			if (this.configuration.hasStatement(id, false)) {
				MappedStatement previous = this.configuration.getMappedStatement(id, false); // issue #2
				if (previous.getDatabaseId() != null) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 获取 LanguageDriver  然后通过这个类 来获取 相关的 操作
	 * @param lang
	 * @return
	 */
	private LanguageDriver getLanguageDriver(String lang) {
		Class<?> langClass = null;
		if (lang != null) {
			langClass = resolveClass(lang);
		}
		return builderAssistant.getLanguageDriver(langClass);
	}

}

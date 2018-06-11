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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Frank D. Martinez [mnesarco]
 * XMLInclude 进行转化操作
 * include     包含操作              Include 的是 sql 语句 所有的 sql 语句都会被保存
 * include    中的refid     就是    sql语句 中的id  找过来的
 *
 * 所以想搞明白这些，一定要对 mybatis的xml   熟练使用
 */
public class XMLIncludeTransformer {

	// 配置 和 mapperBuilder辅助

	private final Configuration configuration;
	private final MapperBuilderAssistant builderAssistant;

	public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
		this.configuration = configuration;
		this.builderAssistant = builderAssistant;
	}

	/**
	 * 将 node 获取
	 * @param source
	 */
	public void applyIncludes(Node source) {
		Properties variablesContext = new Properties();
		Properties configurationVariables = configuration.getVariables();
		if (configurationVariables != null) {
			variablesContext.putAll(configurationVariables);
		}
		applyIncludes(source, variablesContext, false);
	}

	/**
	 * Recursively apply includes through all SQL fragments.
	 *
	 * 主要就是递归操作
	 *
	 * @param source           Include node in DOM tree
	 * @param variablesContext Current context for static variables with values
	 *
	 */
	private void applyIncludes(Node source, final Properties variablesContext, boolean included) {
		// 是否 include 哈哈
		if (source.getNodeName().equals("include")) {
			// 通过 refid 进行操作
			Node toInclude = findSqlFragment(getStringAttribute(source, "refid"), variablesContext);
			// 填充的 properties
			Properties toIncludeContext = getVariablesContext(source, variablesContext);
			// 递归进行操作
			applyIncludes(toInclude, toIncludeContext, true);
			if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
				toInclude = source.getOwnerDocument().importNode(toInclude, true);
			}
			source.getParentNode().replaceChild(toInclude, source);
			while (toInclude.hasChildNodes()) {
				// 将 include 的的内容 进行替换 refid
				toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
			}
			// 将 include 节点移除
			toInclude.getParentNode().removeChild(toInclude);
		} else if (source.getNodeType() == Node.ELEMENT_NODE) {
			if (included && !variablesContext.isEmpty()) {
				// replace variables in attribute values
				NamedNodeMap attributes = source.getAttributes();
				for (int i = 0; i < attributes.getLength(); i++) {
					Node attr = attributes.item(i);
					attr.setNodeValue(PropertyParser.parse(attr.getNodeValue(), variablesContext));
				}
			}
			NodeList children = source.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				applyIncludes(children.item(i), variablesContext, included);
			}
		} else if (included && source.getNodeType() == Node.TEXT_NODE
				&& !variablesContext.isEmpty()) {
			// replace variables in text node
			source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
		}
	}

	/**
	 * 通过 refid 进行解析 添加操作
	 * @param refid
	 * @param variables
	 * @return
	 */
	private Node findSqlFragment(String refid, Properties variables) {
		refid = PropertyParser.parse(refid, variables);
		refid = builderAssistant.applyCurrentNamespace(refid, true);
		try {
			XNode nodeToInclude = configuration.getSqlFragments().get(refid);
			return nodeToInclude.getNode().cloneNode(true);
		} catch (IllegalArgumentException e) {
			throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
		}
	}

	private String getStringAttribute(Node node, String name) {
		return node.getAttributes().getNamedItem(name).getNodeValue();
	}

	/**
	 * Read placeholders and their values from include node definition.
	 * 获取到 node 中需要进行替换的 properties
	 *
	 * @param node                      Include node instance
	 * @param inheritedVariablesContext Current context used for replace variables in new variables values
	 * @return variables context from include instance (no inherited values)
	 */
	private Properties getVariablesContext(Node node, Properties inheritedVariablesContext) {
		Map<String, String> declaredProperties = null;
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			// 只要是 元素节点 就获取存储
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				// 拿到所有的 xml 节点 并装起来   放到peoperties中
				String name = getStringAttribute(n, "name");
				// Replace variables inside
				String value = PropertyParser.parse(getStringAttribute(n, "value"), inheritedVariablesContext);
				if (declaredProperties == null) {
					declaredProperties = new HashMap<String, String>();
				}
				if (declaredProperties.put(name, value) != null) {
					throw new BuilderException("Variable " + name + " defined twice in the same include definition");
				}
			}
		}
		if (declaredProperties == null) {
			return inheritedVariablesContext;
		} else {
			Properties newProperties = new Properties();
			newProperties.putAll(inheritedVariablesContext);
			newProperties.putAll(declaredProperties);
			return newProperties;
		}
	}
}

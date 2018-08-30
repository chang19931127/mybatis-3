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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Clinton Begin
 * 继承了BaseBuilder BaseBuilder里面是一大堆工具方法
 */
public class XMLScriptBuilder extends BaseBuilder {

	/**
	 * XNode 节点
	 */
	private final XNode context;
	/**
	 * 是否动态
	 */
	private boolean isDynamic;
	/**
	 * 参数类型
	 */
	private final Class<?> parameterType;
	/**
	 * 元素节点处理的 handler Map
	 */
	private final Map<String, NodeHandler> nodeHandlerMap = new HashMap<String, NodeHandler>();

	public XMLScriptBuilder(Configuration configuration, XNode context) {
		this(configuration, context, null);
	}

	/**
	 * 核心构造函数
	 * @param configuration
	 * @param context
	 * @param parameterType
	 */
	public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType) {
		super(configuration);
		this.context = context;
		this.parameterType = parameterType;
		initNodeHandlerMap();
	}


	private void initNodeHandlerMap() {
		// 全部都是内部类啊
		// 通过这些操作，可以帮你通过xml 拼接成Sql语句啊
		// 你会发现你写的xml最后规规矩矩的生成Sql了
		// 修建
		nodeHandlerMap.put("trim", new TrimHandler());
		// where
		nodeHandlerMap.put("where", new WhereHandler());
		// set
		nodeHandlerMap.put("set", new SetHandler());
		// foreach
		nodeHandlerMap.put("foreach", new ForEachHandler());
		// if
		nodeHandlerMap.put("if", new IfHandler());
		// choose
		nodeHandlerMap.put("choose", new ChooseHandler());
		// when
		nodeHandlerMap.put("when", new IfHandler());
		// otherwise
		nodeHandlerMap.put("otherwise", new OtherwiseHandler());
		// bind
		nodeHandlerMap.put("bind", new BindHandler());
	}

	public SqlSource parseScriptNode() {
		MixedSqlNode rootSqlNode = parseDynamicTags(context);
		SqlSource sqlSource = null;
		if (isDynamic) {
			sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
		} else {
			sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
		}
		return sqlSource;
	}

	/**
	 * 解析动态标签
	 * xml 中 动态sql 的标签，进行解析
	 * @param node
	 * @return
	 */
	protected MixedSqlNode parseDynamicTags(XNode node) {
		List<SqlNode> contents = new ArrayList<SqlNode>();
		NodeList children = node.getNode().getChildNodes();
		// 遍历所有孩子
		for (int i = 0; i < children.getLength(); i++) {
			XNode child = node.newXNode(children.item(i));
			//
			if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE || child.getNode().getNodeType() == Node.TEXT_NODE) {
				// 值节点
				String data = child.getStringBody("");
				TextSqlNode textSqlNode = new TextSqlNode(data);
				// 只要节点里面有 ${} 就是动态sql
				if (textSqlNode.isDynamic()) {
					contents.add(textSqlNode);
					isDynamic = true;
				} else {
					contents.add(new StaticTextSqlNode(data));
				}
			} else if (child.getNode().getNodeType() == Node.ELEMENT_NODE) { // issue #628
				// 元素节点
				String nodeName = child.getNode().getNodeName();
				NodeHandler handler = nodeHandlerMap.get(nodeName);
				if (handler == null) {
					throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement.");
				}
				// 直接通过元素节点来处理
				handler.handleNode(child, contents);
				// mybatis 定义到 nodeHandlerMap 中的节点都是动态sql
				isDynamic = true;
			}
		}
		return new MixedSqlNode(contents);
	}

	/**
	 * 内部接口 节点处理类
	 */
	private interface NodeHandler {
		/**
		 * 一个方法 处理节点 节点,对应的目标内容
		 * @param nodeToHandle
		 * @param targetContents
		 */
		void handleNode(XNode nodeToHandle, List<SqlNode> targetContents);
	}

	/**
	 * Bind handler
	 */
	private class BindHandler implements NodeHandler {
		public BindHandler() {
			// Prevent Synthetic Access
		}

		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			final String name = nodeToHandle.getStringAttribute("name");
			final String expression = nodeToHandle.getStringAttribute("value");
			// name value 啊
			final VarDeclSqlNode node = new VarDeclSqlNode(name, expression);
			targetContents.add(node);
		}
	}

	/**
	 * 前后修建Handler
	 */
	private class TrimHandler implements NodeHandler {
		public TrimHandler() {
			// Prevent Synthetic Access
		}

		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			String prefix = nodeToHandle.getStringAttribute("prefix");
			String prefixOverrides = nodeToHandle.getStringAttribute("prefixOverrides");
			String suffix = nodeToHandle.getStringAttribute("suffix");
			// 拿到四个值
			String suffixOverrides = nodeToHandle.getStringAttribute("suffixOverrides");
			// 构造一个TrimSqlNode
			TrimSqlNode trim = new TrimSqlNode(configuration, mixedSqlNode, prefix, prefixOverrides, suffix, suffixOverrides);
			// add到list中
			targetContents.add(trim);
		}
	}

	/**
	 * WhereHandler
	 */
	private class WhereHandler implements NodeHandler {
		public WhereHandler() {
			// Prevent Synthetic Access
		}

		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			WhereSqlNode where = new WhereSqlNode(configuration, mixedSqlNode);
			targetContents.add(where);
		}
	}

	/**
	 * SetHandler
	 */
	private class SetHandler implements NodeHandler {
		public SetHandler() {
			// Prevent Synthetic Access
		}

		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			SetSqlNode set = new SetSqlNode(configuration, mixedSqlNode);
			targetContents.add(set);
		}
	}

	private class ForEachHandler implements NodeHandler {
		public ForEachHandler() {
			// Prevent Synthetic Access
		}

		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			String collection = nodeToHandle.getStringAttribute("collection");
			String item = nodeToHandle.getStringAttribute("item");
			String index = nodeToHandle.getStringAttribute("index");
			String open = nodeToHandle.getStringAttribute("open");
			String close = nodeToHandle.getStringAttribute("close");
			String separator = nodeToHandle.getStringAttribute("separator");
			// foreach 的内容就多了,去解析操作吧
			ForEachSqlNode forEachSqlNode = new ForEachSqlNode(configuration, mixedSqlNode, collection, index, item, open, close, separator);
			targetContents.add(forEachSqlNode);
		}
	}

	/**
	 * if handler
	 */
	private class IfHandler implements NodeHandler {
		public IfHandler() {
			// Prevent Synthetic Access
		}

		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			String test = nodeToHandle.getStringAttribute("test");
			// 直接表达式是否成功吧
			IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
			targetContents.add(ifSqlNode);
		}
	}

	/**
	 * 其他情况
	 */
	private class OtherwiseHandler implements NodeHandler {
		public OtherwiseHandler() {
			// Prevent Synthetic Access
		}

		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			targetContents.add(mixedSqlNode);
		}
	}

	/**
	 * ChooseHandler
	 * choose when otherwise 一起配合的把
	 * 就是 if else  明白吧
	 * 父子
	 * choose 表示使用 if else
	 * when if
	 * otherwise else if
	 * otherwise
	 */
	private class ChooseHandler implements NodeHandler {
		public ChooseHandler() {
			// Prevent Synthetic Access
		}

		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			List<SqlNode> whenSqlNodes = new ArrayList<SqlNode>();
			List<SqlNode> otherwiseSqlNodes = new ArrayList<SqlNode>();
			handleWhenOtherwiseNodes(nodeToHandle, whenSqlNodes, otherwiseSqlNodes);
			SqlNode defaultSqlNode = getDefaultSqlNode(otherwiseSqlNodes);
			ChooseSqlNode chooseSqlNode = new ChooseSqlNode(whenSqlNodes, defaultSqlNode);
			targetContents.add(chooseSqlNode);
		}

		/**
		 * 解决when other 节点 和choose的关系
		 * @param chooseSqlNode
		 * @param ifSqlNodes
		 * @param defaultSqlNodes
		 */
		private void handleWhenOtherwiseNodes(XNode chooseSqlNode, List<SqlNode> ifSqlNodes, List<SqlNode> defaultSqlNodes) {
			List<XNode> children = chooseSqlNode.getChildren();
			for (XNode child : children) {
				String nodeName = child.getNode().getNodeName();
				NodeHandler handler = nodeHandlerMap.get(nodeName);
				if (handler instanceof IfHandler) {
					handler.handleNode(child, ifSqlNodes);
				} else if (handler instanceof OtherwiseHandler) {
					handler.handleNode(child, defaultSqlNodes);
				}
			}
		}

		/**
		 * 默认的node
		 * @param defaultSqlNodes
		 * @return
		 */
		private SqlNode getDefaultSqlNode(List<SqlNode> defaultSqlNodes) {
			SqlNode defaultSqlNode = null;
			if (defaultSqlNodes.size() == 1) {
				defaultSqlNode = defaultSqlNodes.get(0);
			} else if (defaultSqlNodes.size() > 1) {
				throw new BuilderException("Too many default (otherwise) elements in choose statement.");
			}
			return defaultSqlNode;
		}
	}

}

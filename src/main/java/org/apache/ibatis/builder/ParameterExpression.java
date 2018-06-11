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
package org.apache.ibatis.builder;

import java.util.HashMap;

/**
 * Inline parameter expression parser. Supported grammar (simplified):
 *
 * <pre>
 * inline-parameter = (propertyName | expression) oldJdbcType attributes
 * propertyName = /expression language's property navigation path/
 * expression = '(' /expression language's expression/ ')'
 * oldJdbcType = ':' /any valid jdbc type/
 * attributes = (',' attribute)*
 * attribute = name '=' value
 * </pre>
 *
 * @author Frank D. Martinez [mnesarco]
 * 内敛参数表达式          #{}      中的参数表达式把
 * 参数表达式 集合类
 * 这里面装了常用的 一些表达式的操作把,方便获得
 * 思考点,为什么要单独抽成一个类,而不是封装到一个属性中
 * 表达式类型,属性,属性和jdbc类型,属性和值,多属性分开
 * ()表达式    : 跟jdbc类型      ,跟属性值       属性=值
 * 针对解析这种操作,就是 if else 然后分步骤
 *
 * 0x20   是一个 空格    大于这个32 进行字符阶段么
 * 这个表达式是针对参数
 */
public class ParameterExpression extends HashMap<String, String> {

	private static final long serialVersionUID = -2417552199605158680L;

	public ParameterExpression(String expression) {
		parse(expression);
	}

	private void parse(String expression) {
		int p = skipWS(expression, 0);
		// 是否是 (
		if (expression.charAt(p) == '(') {
			// 表达式的
			expression(expression, p + 1);
		} else {
			// 属性操作
			property(expression, p);
		}
	}

	/**
	 *  针对 ) 的表达式
	 * @param expression
	 * @param left
	 */
	private void expression(String expression, int left) {
		int match = 1;
		int right = left + 1;
		while (match > 0) {
			// 找到 最外层与之匹配的 ）
			if (expression.charAt(right) == ')') {
				match--;
			} else if (expression.charAt(right) == '(') {
				match++;
			}
			right++;
		}
		// put expression 表达式的
		put("expression", expression.substring(left, right - 1));
		jdbcTypeOpt(expression, right);
	}

	private void property(String expression, int left) {
		if (left < expression.length()) {
			int right = skipUntil(expression, left, ",:");
			put("property", trimmedStr(expression, left, right));
			jdbcTypeOpt(expression, right);
		}
	}

	/**
	 * 表达式中 大于 空格的字符 就返回
	 * @param expression
	 * @param p
	 * @return
	 */
	private int skipWS(String expression, int p) {
		for (int i = p; i < expression.length(); i++) {
			if (expression.charAt(i) > 0x20) {
				return i;
			}
		}
		return expression.length();
	}

	/**
	 * 只找去中间的
	 * @param expression
	 * @param p
	 * @param endChars
	 * @return
	 */
	private int skipUntil(String expression, int p, final String endChars) {
		for (int i = p; i < expression.length(); i++) {
			char c = expression.charAt(i);
			if (endChars.indexOf(c) > -1) {
				return i;
			}
		}
		return expression.length();
	}

	/**
	 * 是否还跟jdbc类型
	 * @param expression
	 * @param p
	 */
	private void jdbcTypeOpt(String expression, int p) {
		p = skipWS(expression, p);
		if (p < expression.length()) {
			if (expression.charAt(p) == ':') {
				// 表达式中有：
				jdbcType(expression, p + 1);
			} else if (expression.charAt(p) == ',') {
				//表达式中有,
				option(expression, p + 1);
			} else {
				throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
			}
		}
	}

	/**
	 * Jdbc 类型添加
	 * @param expression
	 * @param p
	 */
	private void jdbcType(String expression, int p) {
		int left = skipWS(expression, p);
		// 到第一个,
		int right = skipUntil(expression, left, ",");
		if (right > left) {
			// 直接put jdbcType类型
			put("jdbcType", trimmedStr(expression, left, right));
		} else {
			throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
		}
		option(expression, right + 1);
	}

	/**
	 * 是否可选 属性
	 * @param expression
	 * @param p
	 */
	private void option(String expression, int p) {
		int left = skipWS(expression, p);
		if (left < expression.length()) {
			// 开始 那 属性=值 ,分割了
			int right = skipUntil(expression, left, "=");
			String name = trimmedStr(expression, left, right);
			left = right + 1;
			right = skipUntil(expression, left, ",");
			String value = trimmedStr(expression, left, right);
			put(name, value);
			// 递归拿到所有 put
			option(expression, right + 1);
		}
	}

	/**
	 * start 和 end 中的 字符内容
	 * @param str
	 * @param start
	 * @param end
	 * @return
	 */
	private String trimmedStr(String str, int start, int end) {
		while (str.charAt(start) <= 0x20) {
			start++;
		}
		while (str.charAt(end - 1) <= 0x20) {
			end--;
		}
		return start >= end ? "" : str.substring(start, end);
	}

}

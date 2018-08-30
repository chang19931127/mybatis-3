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
package org.apache.ibatis.parsing;

/**
 * @author Clinton Begin
 * Generic Token解析
 * 个人理解,就是广泛的Token解析类把 主要就是 openToken 和closeToken 并且去掉 \转义符号
 * 其实就是解析某种结构的文本了
 * 例如
 * <>   这样的可以进行解析
 * <></> 这样的也可以解析,只是他们的openToken和closeToken不同 然后Handler进行一波操作
 * 这样就可以轻松解析xml中的sql文本了把,个人猜测的用法,
 */
public class GenericTokenParser {

	/**
	 *
	 */
	private final String openToken;
	private final String closeToken;
	private final TokenHandler handler;

	public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
		// 构造函数了,三个参数 open close 以及处理的handler
		this.openToken = openToken;
		this.closeToken = closeToken;
		// handleToken(String)
		this.handler = handler;
	}

	/**
	 * 主要的解析方法
	 * @param text
	 * @return
	 */
	public String parse(String text) {
		// null 或者Empty 就返回""
		if (text == null || text.isEmpty()) {
			return "";
		}
		// search open token
		int start = text.indexOf(openToken, 0);
		// 没有openToken直接原样返回
		if (start == -1) {
			return text;
		}
		// 获得Token的char数组    toCharArray() 获得char数组String 底层是char数组
		char[] src = text.toCharArray();
		// 记录数组偏移
		int offset = 0;
		// 最后解析后的对象
		final StringBuilder builder = new StringBuilder();
		StringBuilder expression = null;

		// 开始解析把
		// 第一步拿掉所有的 \ 转义字符,走if逻辑
		// 然后 else中 一个开头对应一个结尾,这样分成多个开头结尾组 在while中进行操作
		// 然后开头和结尾的 去掉,中间的部分传给TokenHandler
		while (start > -1) {
			if (start > 0 && src[start - 1] == '\\') {
				// 开头是\
				// this open token is escaped. remove the backslash and continue.
				// 去除转移 \ 符号
				builder.append(src, offset, start - offset - 1).append(openToken);
				// 修改偏移量
				offset = start + openToken.length();
			} else {
				// found open token. let's search close token.
				if (expression == null) {
					expression = new StringBuilder();
				} else {
					expression.setLength(0);
				}
				// 追加一个openToken
				builder.append(src, offset, start - offset);
				offset = start + openToken.length();
				// 找到claseToken
				int end = text.indexOf(closeToken, offset);
				while (end > -1) {
					// 同样去掉 \ 转义字符
					if (end > offset && src[end - 1] == '\\') {
						// this close token is escaped. remove the backslash and continue.
						expression.append(src, offset, end - offset - 1).append(closeToken);
						offset = end + closeToken.length();
						end = text.indexOf(closeToken, offset);
					} else {
						expression.append(src, offset, end - offset);
						offset = end + closeToken.length();
						break;
					}
				}
				if (end == -1) {
					// close token was not found.
					builder.append(src, start, src.length - start);
					offset = src.length;
				} else {
					builder.append(handler.handleToken(expression.toString()));
					offset = end + closeToken.length();
				}
			}
			// 再次获得openToken的位置
			start = text.indexOf(openToken, offset);
		}
		if (offset < src.length) {
			builder.append(src, offset, src.length - offset);
		}
		return builder.toString();
	}

	public static void main(String[] args) {
		String content = "<div id='1' /> <html name='kaixin'/>";
		GenericTokenParser tokenParser = new GenericTokenParser("<","/>",(var) -> {
			// div id = '1'
			// html name='kaixin'
			System.out.println(var);
			return "-";
		});
		// - - 解析后都替换成- 两组
		System.out.println(tokenParser.parse(content));
		String s1 = "${name}";
		GenericTokenParser tokenParser1 = new GenericTokenParser("${","}",(var) -> {
			if("name".equals(var)){
				return "常振东";
			}
			return "-";
		});
		// 这不是一个简单的站位符就被替换了么
		System.out.println(tokenParser1.parse(s1));
	}
}

/**
 * Copyright 2009-2018 the original author or authors.
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
package org.apache.ibatis.scripting.xmltags;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.BuilderException;

/**
 * @author Clinton Begin
 * 表达式解析器
 * 通过ognl 的fori 进行一个操作 看是否还能继续 获得迭代器
 */
public class ExpressionEvaluator {

	/**
	 * 计算是否 boolean
	 * @param expression
	 * @param parameterObject
	 * @return
	 */
	public boolean evaluateBoolean(String expression, Object parameterObject) {
		Object value = OgnlCache.getValue(expression, parameterObject);
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof Number) {
			return new BigDecimal(String.valueOf(value)).compareTo(BigDecimal.ZERO) != 0;
		}
		return value != null;
	}

	/**
	 * 是否还可以迭代
	 * @param expression
	 * @param parameterObject
	 * @return
	 */
	public Iterable<?> evaluateIterable(String expression, Object parameterObject) {
		Object value = OgnlCache.getValue(expression, parameterObject);
		if (value == null) {
			throw new BuilderException("The expression '" + expression + "' evaluated to a null value.");
		}
		if (value instanceof Iterable) {
			return (Iterable<?>) value;
		}
		if (value.getClass().isArray()) {
			// the array may be primitive, so Arrays.asList() may throw
			// a ClassCastException (issue 209).  Do the work manually
			// Curse primitives! :) (JGB)
			int size = Array.getLength(value);
			List<Object> answer = new ArrayList<Object>();
			for (int i = 0; i < size; i++) {
				Object o = Array.get(value, i);
				answer.add(o);
			}
			return answer;
		}
		if (value instanceof Map) {
			return ((Map) value).entrySet();
		}
		throw new BuilderException("Error evaluating expression '" + expression + "'.  Return value (" + value + ") was not iterable.");
	}

}

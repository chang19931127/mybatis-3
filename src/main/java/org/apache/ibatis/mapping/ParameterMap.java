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
package org.apache.ibatis.mapping;

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 * 参数的封装集合
 */
public class ParameterMap {

	/**
	 * 一个参数对应 一个 id
	 */
	private String id;
	/**
	 * 对应的java类型
	 */
	private Class<?> type;
	/**
	 * 对应的参数 mapping 这个集合是不可变的。通过Collections 返回的
	 */
	private List<ParameterMapping> parameterMappings;

	private ParameterMap() {
	}

	public static class Builder {
		private ParameterMap parameterMap = new ParameterMap();

		public Builder(Configuration configuration, String id, Class<?> type, List<ParameterMapping> parameterMappings) {
			parameterMap.id = id;
			parameterMap.type = type;
			parameterMap.parameterMappings = parameterMappings;
		}

		public Class<?> type() {
			return parameterMap.type;
		}

		public ParameterMap build() {
			//lock down collections
			parameterMap.parameterMappings = Collections.unmodifiableList(parameterMap.parameterMappings);
			return parameterMap;
		}
	}

	public String getId() {
		return id;
	}

	public Class<?> getType() {
		return type;
	}

	public List<ParameterMapping> getParameterMappings() {
		return parameterMappings;
	}

}

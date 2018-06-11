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
package org.apache.ibatis.builder;

import java.util.List;

import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;

/**
 * @author Eduardo Macarron
 * 应该就是一个结果集Map 解决
 */
public class ResultMapResolver {
	/**
	 * Mapper 构造助理类
	 */
	private final MapperBuilderAssistant assistant;
	/**
	 * id
	 */
	private final String id;
	/**
	 * 类型
	 */
	private final Class<?> type;
	/**
	 * 继承
	 */
	private final String extend;
	private final Discriminator discriminator;
	/**
	 * ResultMapping list
	 */
	private final List<ResultMapping> resultMappings;
	/**
	 * 是否自动映射
	 */
	private final Boolean autoMapping;

	public ResultMapResolver(MapperBuilderAssistant assistant, String id, Class<?> type, String extend, Discriminator discriminator, List<ResultMapping> resultMappings, Boolean autoMapping) {
		this.assistant = assistant;
		this.id = id;
		this.type = type;
		this.extend = extend;
		this.discriminator = discriminator;
		this.resultMappings = resultMappings;
		this.autoMapping = autoMapping;
	}

	/**
	 * 主要是这个方法把
	 * @return
	 */
	public ResultMap resolve() {
		// 直接将结果集添加到MapperBuilderAssistan 类中
		return assistant.addResultMap(this.id, this.type, this.extend, this.discriminator, this.resultMappings, this.autoMapping);
	}

}
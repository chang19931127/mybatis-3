/**
 * Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.mapping;

import java.util.Collections;
import java.util.Map;

import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 * 一个鉴别器
 * 鉴别 ResultMapping 的情况
 *
 * 主要用在 级联的时候保证映射什么类型
 *
 * 有一个这样的 xml <discriminator></discriminator>
 * 声明在结果集中的
 */
public class Discriminator {

	/**
	 * 鉴别的对象
	 */
	private ResultMapping resultMapping;

	/**
	 * ResultMapping  映射结果集
	 */
	private Map<String, String> discriminatorMap;

	Discriminator() {
	}

	public static class Builder {
		private Discriminator discriminator = new Discriminator();

		public Builder(Configuration configuration, ResultMapping resultMapping, Map<String, String> discriminatorMap) {
			discriminator.resultMapping = resultMapping;
			discriminator.discriminatorMap = discriminatorMap;
		}

		public Discriminator build() {
			assert discriminator.resultMapping != null;
			assert discriminator.discriminatorMap != null;
			assert !discriminator.discriminatorMap.isEmpty();
			//lock down map
			discriminator.discriminatorMap = Collections.unmodifiableMap(discriminator.discriminatorMap);
			return discriminator;
		}
	}

	public ResultMapping getResultMapping() {
		return resultMapping;
	}

	public Map<String, String> getDiscriminatorMap() {
		return discriminatorMap;
	}

	public String getMapIdFor(String s) {
		return discriminatorMap.get(s);
	}

}

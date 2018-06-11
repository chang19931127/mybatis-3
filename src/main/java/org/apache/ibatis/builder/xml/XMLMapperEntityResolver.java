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
package org.apache.ibatis.builder.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.apache.ibatis.io.Resources;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Offline entity resolver for the MyBatis DTDs
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 * XML 对象发现    EntityResolver 这个接口很巧妙 jdk 提供的api  用来 dtd约束的将dtd进行操作
 * mybatis 主要还是提供给 XPathParse来进行使用的
 * SAX解析器必须这么使用,百度EntityResolver就可以了 就是为了解析自定的的 约束
 * 这里面要自己写一个,就是因为默认实现是通过网络的,所以性能不好   XmlResolver这个类
 * 因此mybatis 就需要自己实现一个类,然后调用自己的本地文件
 * EntityResolver 和 InputSource 这一对
 *
 * 主要是这个API
 * SAXReader reader = new SAXReader();
 * reader.setEntityResolver(new XMLMapperEntityResolver()); // ignore dtd
 */
public class XMLMapperEntityResolver implements EntityResolver {

	// 我们常见的 dtd 约束  老版本和新版本 ibatis,mybatis 的 dtd

	private static final String IBATIS_CONFIG_SYSTEM = "ibatis-3-config.dtd";
	private static final String IBATIS_MAPPER_SYSTEM = "ibatis-3-mapper.dtd";
	private static final String MYBATIS_CONFIG_SYSTEM = "mybatis-3-config.dtd";
	private static final String MYBATIS_MAPPER_SYSTEM = "mybatis-3-mapper.dtd";

	// 路径

	private static final String MYBATIS_CONFIG_DTD = "org/apache/ibatis/builder/xml/mybatis-3-config.dtd";
	private static final String MYBATIS_MAPPER_DTD = "org/apache/ibatis/builder/xml/mybatis-3-mapper.dtd";

	/**
	 * Converts a public DTD into a local one
	 *
	 * @param publicId The public id that is what comes after "PUBLIC"
	 * @param systemId The system id that is what comes after the public id.
	 * @return The InputSource for the DTD
	 *
	 * @throws org.xml.sax.SAXException If anything goes wrong
	 * 主要的就是这个复写方法 一个公共的 一个系统内部的
	 */
	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
		try {
			if (systemId != null) {
				String lowerCaseSystemId = systemId.toLowerCase(Locale.ENGLISH);
				// 是config 还是 mapper
				if (lowerCaseSystemId.contains(MYBATIS_CONFIG_SYSTEM) || lowerCaseSystemId.contains(IBATIS_CONFIG_SYSTEM)) {
					return getInputSource(MYBATIS_CONFIG_DTD, publicId, systemId);
				} else if (lowerCaseSystemId.contains(MYBATIS_MAPPER_SYSTEM) || lowerCaseSystemId.contains(IBATIS_MAPPER_SYSTEM)) {
					return getInputSource(MYBATIS_MAPPER_DTD, publicId, systemId);
				}
			}
			return null;
		} catch (Exception e) {
			throw new SAXException(e.toString());
		}
	}

	private InputSource getInputSource(String path, String publicId, String systemId) {
		InputSource source = null;
		if (path != null) {
			try {
				// 注解封装InputSource对象并且返回
				InputStream in = Resources.getResourceAsStream(path);
				source = new InputSource(in);
				source.setPublicId(publicId);
				source.setSystemId(systemId);
			} catch (IOException e) {
				// ignore, null is ok
			}
		}
		return source;
	}

}
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
package org.apache.ibatis.datasource.unpooled;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * @author Clinton Begin
 * 简单数据源工厂
 */
public class UnpooledDataSourceFactory implements DataSourceFactory {

	/**
	 * 前缀 driver.
	 */
	private static final String DRIVER_PROPERTY_PREFIX = "driver.";
	/**
	 * 前缀的长度
	 */
	private static final int DRIVER_PROPERTY_PREFIX_LENGTH = DRIVER_PROPERTY_PREFIX.length();

	protected DataSource dataSource;

	public UnpooledDataSourceFactory() {
		this.dataSource = new UnpooledDataSource();
	}

	@Override
	public void setProperties(Properties properties) {
		// 直接通过配置文件
		Properties driverProperties = new Properties();
		// 直接获得dataSource的反射相关信息
		MetaObject metaDataSource = SystemMetaObject.forObject(dataSource);
		for (Object key : properties.keySet()) {
			// 开始对配置进行操作了
			String propertyName = (String) key;
			if (propertyName.startsWith(DRIVER_PROPERTY_PREFIX)) {
				String value = properties.getProperty(propertyName);
				driverProperties.setProperty(propertyName.substring(DRIVER_PROPERTY_PREFIX_LENGTH), value);
			} else if (metaDataSource.hasSetter(propertyName)) {
				String value = (String) properties.get(propertyName);
				Object convertedValue = convertValue(metaDataSource, propertyName, value);
				metaDataSource.setValue(propertyName, convertedValue);
			} else {
				throw new DataSourceException("Unknown DataSource property: " + propertyName);
			}
		}
		// 直接将配置文件set到DataSource中
		if (driverProperties.size() > 0) {
			metaDataSource.setValue("driverProperties", driverProperties);
		}
	}

	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * 使用的还是包装类啊
	 * 直接set操作啊
	 * @param metaDataSource 对象啊
	 * @param propertyName key
	 * @param value value
	 * @return
	 */
	private Object convertValue(MetaObject metaDataSource, String propertyName, String value) {
		Object convertedValue = value;
		Class<?> targetType = metaDataSource.getSetterType(propertyName);
		if (targetType == Integer.class || targetType == int.class) {
			convertedValue = Integer.valueOf(value);
		} else if (targetType == Long.class || targetType == long.class) {
			convertedValue = Long.valueOf(value);
		} else if (targetType == Boolean.class || targetType == boolean.class) {
			convertedValue = Boolean.valueOf(value);
		}
		return convertedValue;
	}

}

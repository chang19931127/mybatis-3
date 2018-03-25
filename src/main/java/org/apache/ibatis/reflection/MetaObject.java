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
package org.apache.ibatis.reflection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
import org.apache.ibatis.reflection.wrapper.MapWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * @author Clinton Begin
 * 对象的源信息
 * 类似于jdbc中的元信息一样,哈哈
 *
 */
public class MetaObject {
	/**
	 * 原始对象
	 */
	private final Object originalObject;
	/**
	 * 包装对象 这个里面有很多关于对象的操作
	 */
	private final ObjectWrapper objectWrapper;
	/**
	 * 创建对象的工厂
	 */
	private final ObjectFactory objectFactory;
	/**
	 * 包装对象创建的工厂
	 */
	private final ObjectWrapperFactory objectWrapperFactory;
	/**
	 * 反射工厂
	 */
	private final ReflectorFactory reflectorFactory;

	// 构造器私有化
	private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
		this.originalObject = object;
		this.objectFactory = objectFactory;
		this.objectWrapperFactory = objectWrapperFactory;
		this.reflectorFactory = reflectorFactory;

		// 选择合适的包装对象
		if (object instanceof ObjectWrapper) {
			this.objectWrapper = (ObjectWrapper) object;
		} else if (objectWrapperFactory.hasWrapperFor(object)) {
			this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
		} else if (object instanceof Map) {
			this.objectWrapper = new MapWrapper(this, (Map) object);
		} else if (object instanceof Collection) {
			this.objectWrapper = new CollectionWrapper(this, (Collection) object);
		} else {
			this.objectWrapper = new BeanWrapper(this, object);
		}
	}

	//通过forObject来进行操作
	public static MetaObject forObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
		if (object == null) {
			// 这个操作兜了一圈又回来了 不过能保证单例
			return SystemMetaObject.NULL_META_OBJECT;
		} else {
			return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
		}
	}

	public ObjectFactory getObjectFactory() {
		return objectFactory;
	}

	public ObjectWrapperFactory getObjectWrapperFactory() {
		return objectWrapperFactory;
	}

	public ReflectorFactory getReflectorFactory() {
		return reflectorFactory;
	}

	public Object getOriginalObject() {
		return originalObject;
	}

	public String findProperty(String propName, boolean useCamelCaseMapping) {
		return objectWrapper.findProperty(propName, useCamelCaseMapping);
	}

	public String[] getGetterNames() {
		return objectWrapper.getGetterNames();
	}

	public String[] getSetterNames() {
		return objectWrapper.getSetterNames();
	}

	public Class<?> getSetterType(String name) {
		return objectWrapper.getSetterType(name);
	}

	public Class<?> getGetterType(String name) {
		return objectWrapper.getGetterType(name);
	}

	public boolean hasSetter(String name) {
		return objectWrapper.hasSetter(name);
	}

	public boolean hasGetter(String name) {
		return objectWrapper.hasGetter(name);
	}

	// 工具不就使用上了,“a.b” 拿到最终太对象的Object包装
	// 都是为了拿到包装对象 关于那个词法操作的
	// 这么做的目的我猜测,封装完毕后,就可以构造出来对象了a.b.c 那不就是 A 一个属性 B 中的一个属性
	public Object getValue(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
			if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
				return null;
			} else {
				//递归了
				return metaValue.getValue(prop.getChildren());
			}
		} else {
			return objectWrapper.get(prop);
		}
	}

	// 针对词法操作set 包装类型的
	public void setValue(String name, Object value) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
			if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
				if (value == null && prop.getChildren() != null) {
					// don't instantiate child path if value is null
					return;
				} else {
					metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
				}
			}
			metaValue.setValue(prop.getChildren(), value);
		} else {
			objectWrapper.set(prop, value);
		}
	}

	public MetaObject metaObjectForProperty(String name) {
		Object value = getValue(name);
		return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
	}

	public ObjectWrapper getObjectWrapper() {
		return objectWrapper;
	}

	public boolean isCollection() {
		return objectWrapper.isCollection();
	}

	public void add(Object element) {
		objectWrapper.add(element);
	}

	public <E> void addAll(List<E> list) {
		objectWrapper.addAll(list);
	}

}

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
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * @author Clinton Begin
 * 比较简单的属性语法解析
 * 主要两种语法
 * x.y
 * x[y]
 * 通过四个内置变量来进行存储,方便使用
 * 最终复杂的操作就是a.b[1].c     代表了很深的内容把
 * 很多反射操作都是围绕这个类进行的    那么这个类所对应的词法写在那里？
 */
public class PropertyTokenizer implements Iterator<PropertyTokenizer> {
	/**
	 * x
	 */
	private String name;
	/**
	 * x[y]
	 */
	private final String indexedName;
	/**
	 * x[y]  中的y
	 */
	private String index;
	/**
	 * x.y   中的y
	 */
	private final String children;

	public PropertyTokenizer(String fullname) {
		int delim = fullname.indexOf('.');
		if (delim > -1) {
			name = fullname.substring(0, delim);
			children = fullname.substring(delim + 1);
		} else {
			name = fullname;
			children = null;
		}
		indexedName = name;
		delim = name.indexOf('[');
		if (delim > -1) {
			index = name.substring(delim + 1, name.length() - 1);
			name = name.substring(0, delim);
		}
	}

	public String getName() {
		return name;
	}

	public String getIndex() {
		return index;
	}

	public String getIndexedName() {
		return indexedName;
	}

	public String getChildren() {
		return children;
	}

	@Override
	public boolean hasNext() {
		return children != null;
	}

	@Override
	public PropertyTokenizer next() {
		return new PropertyTokenizer(children);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
	}

	public static void main(String[] args) {
		PropertyTokenizer propertyTokenizer = new PropertyTokenizer("a.b");
		System.out.println(propertyTokenizer.name+propertyTokenizer.children+propertyTokenizer.index+propertyTokenizer.indexedName);
		propertyTokenizer = new PropertyTokenizer("a[12]");
		System.out.println(propertyTokenizer.name+propertyTokenizer.children+propertyTokenizer.index+propertyTokenizer.indexedName);
		propertyTokenizer = new PropertyTokenizer("a.c[12].a.c");
		System.out.println(propertyTokenizer.name+propertyTokenizer.children+propertyTokenizer.index+propertyTokenizer.indexedName);
		System.out.println(propertyTokenizer.hasNext());

	}
}

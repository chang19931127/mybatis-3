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

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * @author Clinton Begin
 * 获得属性名 必须set方法,get方法,is方法才算
 */
public final class PropertyNamer {

	private PropertyNamer() {
		// Prevent Instantiation of Static Class
	}

	/**
	 * 通过get set is 方法来获得属性名
	 * 其实就是生成get set is方法的反推
	 * @param name 方法名
	 * @return
	 */
	public static String methodToProperty(String name) {
		// 用到了String.substring() 从传入的位置开始截取
		if (name.startsWith("is")) {
			name = name.substring(2);
		} else if (name.startsWith("get") || name.startsWith("set")) {
			name = name.substring(3);
		} else {
			throw new ReflectionException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
		}

		if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
			// 一个字母 || 多个字母,第二个字母小写   大写变小写   getAba    =  aba
			name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
		}
		// setAA 直接返回 AA    因此Spring中也提到,属性中前两个字母同大小写,否则反射有问题
		// 属性里面是aA   得到的set是 setAA   结果反推回来是AA那么就有问题了
		return name;
	}

	public static boolean isProperty(String name) {
		return name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
	}

	public static boolean isGetter(String name) {
		return name.startsWith("get") || name.startsWith("is");
	}

	public static boolean isSetter(String name) {
		return name.startsWith("set");
	}

	public static void main(String[] args) {
		System.out.println(methodToProperty("setAA"));
		System.out.println(methodToProperty("setAa"));
		System.out.println(methodToProperty("setaA"));
	}

}

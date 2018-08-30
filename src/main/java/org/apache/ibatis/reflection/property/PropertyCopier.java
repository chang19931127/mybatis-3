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

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * @author Clinton Begin
 * 对象属性赋值帮助类
 */
public final class PropertyCopier {

	private PropertyCopier() {
		// Prevent Instantiation of Static Class
	}

	/**
	 * 将一个对象中的属性copy到另外一个属性中
	 * 操作很简单把,就是把Class对象中的所有字段遍历
	 * 然后把source中的copy到dest中
	 * 如果source没有找到就自然的跳过。
	 * 最好的情况下sourceBean,destBean 都是Class类型的这样不会有大问题
	 * @param type                  Class类型
	 * @param sourceBean            原始对象
	 * @param destinationBean       目标对象
	 */
	public static void copyBeanProperties(Class<?> type, Object sourceBean, Object destinationBean) {
		// 将参数内部化,然后在进行局部化操作
		Class<?> parent = type;
		while (parent != null) {
			final Field[] fields = parent.getDeclaredFields();
			for (Field field : fields) {
				try {
					field.setAccessible(true);
					// 目标对象有的才set
					field.set(destinationBean, field.get(sourceBean));
				} catch (Exception e) {
					// Nothing useful to do, will only fail on final fields, which will be ignored.
				}
			}
			parent = parent.getSuperclass();
		}
	}

	private static class Person{
		private String name;
		private int age;

		public Person(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		@Override
		public String toString() {
			return "Person{" +
					"name='" + name + '\'' +
					", age=" + age +
					'}';
		}
	}

	private static class Person1{
		private String name;
		private int age;

		public Person1(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		@Override
		public String toString() {
			return "Person1{" +
					"name='" + name + '\'' +
					", age=" + age +
					'}';
		}
	}

	public static void main(String[] args) {
		Person p = new Person("开心",123);
		Person person = new Person("开心",12);
		Object obj = new Object();
		copyBeanProperties(Person.class,p,person);
		System.out.println(person);
	}

}

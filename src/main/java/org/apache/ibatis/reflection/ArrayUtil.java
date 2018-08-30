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
package org.apache.ibatis.reflection;

import java.util.Arrays;

/**
 * Provides hashCode, equals and toString methods that can handle array.
 * 关于数组操作的工具类,查看类结构可以了解到三个方法,hashCode,equals and toString
 * 需要借助反射的操作
 * 方法针对所有类型,但是会对数组做特殊的处理,也就是隐形约束如参数组
 * 其实确切的说就是对Arrays 数组工具类做了一层封装
 * @author 常振东增加注释
 */
public class ArrayUtil {

	/**
	 * Returns a hash code for {@code obj}.
	 * 返回hashCode,hashCode有一些约束问题和equal方法.    equal相等hashcode一定要等 约束哈
	 * 这里结构是否都可以考虑成
	 *
	 * if(){
	 *     return 0;
	 * }
	 *
	 * @param obj
	 *          The object to get a hash code for. May be an array or <code>null</code>.
	 * @return A hash code of {@code obj} or 0 if {@code obj} is <code>null</code>
	 */
	public static int hashCode(Object obj) {
		// 先校验null 0返回
		if (obj == null) {
			// for consistency with Arrays#hashCode() and Objects#hashCode()
			return 0;
		}
		// 获得字节码,字节码不变 final修饰
		final Class<?> clazz = obj.getClass();
		// 如果不是数组,直接obj.hashCode
		if (!clazz.isArray()) {
			return obj.hashCode();
		}
		// 获取到这个字节信息的组成信息,这个字节信息如果不是数组就返回null,是就返回数据的类型
		final Class<?> componentType = clazz.getComponentType();
		/**
		 * 可以修改成
		 * if(){
		 *     return 0;
		 * }
		 * if(){
		 *     return 0;
		 * }
		 * 原生类型也是有字节码的,for example int.class
		 * 借助Arrays.hashCode(数组对象)来进行获得hashCode
		 * hashCode 类似String中  31*X 累加
 		 */
		if (long.class.equals(componentType)) {
			return Arrays.hashCode((long[]) obj);
		} else if (int.class.equals(componentType)) {
			return Arrays.hashCode((int[]) obj);
		} else if (short.class.equals(componentType)) {
			return Arrays.hashCode((short[]) obj);
		} else if (char.class.equals(componentType)) {
			return Arrays.hashCode((char[]) obj);
		} else if (byte.class.equals(componentType)) {
			return Arrays.hashCode((byte[]) obj);
		} else if (boolean.class.equals(componentType)) {
			return Arrays.hashCode((boolean[]) obj);
		} else if (float.class.equals(componentType)) {
			return Arrays.hashCode((float[]) obj);
		} else if (double.class.equals(componentType)) {
			return Arrays.hashCode((double[]) obj);
		} else {
			return Arrays.hashCode((Object[]) obj);
		}
	}

	/**
	 * Compares two objects. Returns <code>true</code> if
	 * <ul>
	 * <li>{@code thisObj} and {@code thatObj} are both <code>null</code></li>
	 * <li>{@code thisObj} and {@code thatObj} are instances of the same type and
	 * {@link Object#equals(Object)} returns <code>true</code></li>
	 * <li>{@code thisObj} and {@code thatObj} are arrays with the same component type and
	 * equals() method of {@link Arrays} returns <code>true</code> (not deepEquals())</li>
	 * </ul>
	 *
	 * @param thisObj
	 *          The left hand object to compare. May be an array or <code>null</code>
	 * @param thatObj
	 *          The right hand object to compare. May be an array or <code>null</code>
	 * @return <code>true</code> if two objects are equal; <code>false</code> otherwise.
	 *
	 * equals 比较,相同true,同时null或者相同实例
	 */
	public static boolean equals(Object thisObj, Object thatObj) {
		// 同为null true,否则 false
		if (thisObj == null) {
			return thatObj == null;
		} else if (thatObj == null) {
			return false;
		}
		// 不同类型 false
		final Class<?> clazz = thisObj.getClass();
		if (!clazz.equals(thatObj.getClass())) {
			return false;
		}
		// 下面比骄的都是相同类型
		// 不是数组类型，就直接实例去比较 ==
		if (!clazz.isArray()) {
			return thisObj.equals(thatObj);
		}
		final Class<?> componentType = clazz.getComponentType();
		// 拿到数组元素类型,通过Arrays.equals来进行数组判断,数组有特殊比较方式
		if (long.class.equals(componentType)) {
			return Arrays.equals((long[]) thisObj, (long[]) thatObj);
		} else if (int.class.equals(componentType)) {
			return Arrays.equals((int[]) thisObj, (int[]) thatObj);
		} else if (short.class.equals(componentType)) {
			return Arrays.equals((short[]) thisObj, (short[]) thatObj);
		} else if (char.class.equals(componentType)) {
			return Arrays.equals((char[]) thisObj, (char[]) thatObj);
		} else if (byte.class.equals(componentType)) {
			return Arrays.equals((byte[]) thisObj, (byte[]) thatObj);
		} else if (boolean.class.equals(componentType)) {
			return Arrays.equals((boolean[]) thisObj, (boolean[]) thatObj);
		} else if (float.class.equals(componentType)) {
			return Arrays.equals((float[]) thisObj, (float[]) thatObj);
		} else if (double.class.equals(componentType)) {
			return Arrays.equals((double[]) thisObj, (double[]) thatObj);
		} else {
			return Arrays.equals((Object[]) thisObj, (Object[]) thatObj);
		}
	}

	/**
	 * If the {@code obj} is an array, toString() method of {@link Arrays} is called. Otherwise
	 * {@link Object#toString()} is called. Returns "null" if {@code obj} is <code>null</code>.
	 *
	 * @param obj
	 *          An object. May be an array or <code>null</code>.
	 * @return String representation of the {@code obj}.
	 *
	 * toString方法
	 */
	public static String toString(Object obj) {
		// "null"返回
		if (obj == null) {
			return "null";
		}
		final Class<?> clazz = obj.getClass();
		// obj.toString
		if (!clazz.isArray()) {
			return obj.toString();
		}
		final Class<?> componentType = obj.getClass().getComponentType();
		// Arrays.toString [1,2,3] 这种
		if (long.class.equals(componentType)) {
			return Arrays.toString((long[]) obj);
		} else if (int.class.equals(componentType)) {
			return Arrays.toString((int[]) obj);
		} else if (short.class.equals(componentType)) {
			return Arrays.toString((short[]) obj);
		} else if (char.class.equals(componentType)) {
			return Arrays.toString((char[]) obj);
		} else if (byte.class.equals(componentType)) {
			return Arrays.toString((byte[]) obj);
		} else if (boolean.class.equals(componentType)) {
			return Arrays.toString((boolean[]) obj);
		} else if (float.class.equals(componentType)) {
			return Arrays.toString((float[]) obj);
		} else if (double.class.equals(componentType)) {
			return Arrays.toString((double[]) obj);
		} else {
			return Arrays.toString((Object[]) obj);
		}
	}

	public static void main(String[] args) {
		int[] ints = {1,2,3};
		long[] longs = {1L,2L,3L};
		equals(ints,longs);
	}
}

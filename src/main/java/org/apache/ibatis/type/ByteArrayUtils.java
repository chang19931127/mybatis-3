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
package org.apache.ibatis.type;

/**
 * @author Clinton Begin
 * 字节数据工具类
 * 仅限包内使用
 */
class ByteArrayUtils {

	private ByteArrayUtils() {
		// Prevent Instantiation
	}

	/**
	 * 将Byte[]  包装类转化成byte[]原生类  数组对象
	 * @param objects 包装类数据
	 * @return 原生类数据
	 */
	static byte[] convertToPrimitiveArray(Byte[] objects) {
		// 这里为什么使用final 修改  好好想一下 final的优势
		final byte[] bytes = new byte[objects.length];
		for (int i = 0; i < objects.length; i++) {
			// 直接阴式转化
			bytes[i] = objects[i];
		}
		return bytes;
	}

	/**
	 * 将原生类转化成包装类
	 * @param bytes
	 * @return
	 */
	static Byte[] convertToObjectArray(byte[] bytes) {
		final Byte[] objects = new Byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			objects[i] = bytes[i];
		}
		return objects;
	}
}

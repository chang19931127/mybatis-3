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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.lang.UsesJava8;

/**
 * 参数工具类
 * 通过反射获取 可以执行操作的 参数集合
 */
@UsesJava8
public class ParamNameUtil {
	public static List<String> getParamNames(Method method) {
		return getParameterNames(method);
	}

	public static List<String> getParamNames(Constructor<?> constructor) {
		return getParameterNames(constructor);
	}

	/**
	 * 这个方法很通用,被抽取出来,暴露到外面的就是两种操作
	 * 因此，记住抽取出来的方法参数一定要具有一定的适用性
	 * Executable 1.8新添的,是方法和构造器的抽象父类,当然就可以提取很多方法到父类中
	 * 因此可以比较下Method和 1.7中的Method代码量
	 * @param executable
	 * @return
	 */
	private static List<String> getParameterNames(Executable executable) {
		//变量先实例化,不至于null返回
		final List<String> names = new ArrayList<String>();
		//方法内部实现的很棒,通过临时备份来防止并发,并且也不会null返回
		final Parameter[] params = executable.getParameters();
		for (Parameter param : params) {
			names.add(param.getName());
		}
		return names;
	}

	private ParamNameUtil() {
		super();
	}
}

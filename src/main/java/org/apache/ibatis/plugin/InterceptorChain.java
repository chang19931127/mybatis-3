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
package org.apache.ibatis.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Clinton Begin
 * 拦截器链,所有的拦截器都在这里
 */
public class InterceptorChain {

	/**
	 * 按道理这里应该会去存放所有嵌入的拦截器
	 */
	private final List<Interceptor> interceptors = new ArrayList<Interceptor>();

	/**
	 * 当然因为拦截器,需要嵌入进去 如何嵌入就是plugin方法
	 * 所以就是一层一层的嵌套,形成一个链式结构
	 * 简单说就是代理链
	 * @param target
	 * @return
	 */
	public Object pluginAll(Object target) {
		// foreach 层层嵌套
		for (Interceptor interceptor : interceptors) {
			target = interceptor.plugin(target);
		}
		return target;
	}

	/**
	 * 添加拦截器
	 * @param interceptor
	 */
	public void addInterceptor(Interceptor interceptor) {
		interceptors.add(interceptor);
	}

	/**
	 * 拿到所有拦截器操作
	 * @return
	 */
	public List<Interceptor> getInterceptors() {
		return Collections.unmodifiableList(interceptors);
	}

}

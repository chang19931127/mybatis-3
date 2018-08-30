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

import java.util.Properties;

/**
 * @author Clinton Begin
 * 插件,也就是拦截器把,抽象接口
 * 接口配合注解需要同时使用
 */
public interface Interceptor {

	/**
	 * 拦截方法逻辑把,将原有的方法逻辑都封装到Invocation中
	 * @param invocation
	 * @return
	 * @throws Throwable
	 */
	Object intercept(Invocation invocation) throws Throwable;

	/**
	 * plugin 通过拦截target 生成拦截后的代理对象
	 * @param target
	 * @return
	 */
	Object plugin(Object target);

	/**
	 * 可以设置一些参数,插入嵌入的时候带的值
	 * @param properties
	 */
	void setProperties(Properties properties);

}

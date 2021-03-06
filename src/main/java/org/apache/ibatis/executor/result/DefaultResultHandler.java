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
package org.apache.ibatis.executor.result;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

/**
 * @author Clinton Begin
 * 默认的结果处理Handler
 * 直接返回的对象
 */
public class DefaultResultHandler implements ResultHandler<Object> {

	/**
	 * final 修饰 不可变对象
	 */
	private final List<Object> list;

	public DefaultResultHandler() {
		list = new ArrayList<Object>();
	}

	@SuppressWarnings("unchecked")
	public DefaultResultHandler(ObjectFactory objectFactory) {
		list = objectFactory.create(List.class);
	}

	@Override
	public void handleResult(ResultContext<? extends Object> context) {
		// 重点是这个对象，仅仅做了一个add 操作
		// 将结果处理的上下文的对象 取出来 然后添加到 内部封装的List中
		list.add(context.getResultObject());
	}

	public List<Object> getResultList() {
		return list;
	}

}

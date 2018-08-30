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
package org.apache.ibatis.executor.parameter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A parameter handler sets the parameters of the {@code PreparedStatement}
 *
 * 仅仅定义一个接口 参数处理的
 *
 * @author Clinton Begin
 */
public interface ParameterHandler {

	/**
	 * 获取参数对象
	 * @return
	 */
	Object getParameterObject();

	/**
	 * 将参数对象设置到 传入的PreparedStatement     只有PreparedStatement需要处理
	 * @param ps
	 * @throws SQLException
	 */
	void setParameters(PreparedStatement ps)
			throws SQLException;

}

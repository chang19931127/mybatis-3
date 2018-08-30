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
package org.apache.ibatis.executor.keygen;

import java.sql.Statement;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;

/**
 * @author Clinton Begin
 * 主键生成接口
 * 接口为什么定义两个方法    处理前 处理后 ?
 * 为什么需要 Executor
 *          MaperedStatement
 *          Statement
 *          参数
 *
 */
public interface KeyGenerator {

	/**
	 * 执行前
	 * @param executor 执行器
	 * @param ms 映射
	 * @param stmt statement语句
	 * @param parameter 参数
	 */
	void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter);

	/**
	 * 执行后
	 * @param executor 执行器
	 * @param ms 映射
	 * @param stmt statement语句
	 * @param parameter 参数
	 */
	void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter);

}

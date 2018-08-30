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
/**
 * TODO fillme.
 * 日志包,将操作以日志的形式存储
 * 自己统一日志入口Log 异常LogException
 *              日志实现类通过工厂LogFactory
 *
 * 那么多runnable 那么到底set的是哪一个啊
 * 按道理来说肯定是 static 导致所有的Runnable 都执行了
 * 个人理解,都执行但是Classpath里面有哪个包就不会异常,就可以去掉用了
 * 还记得封装的logConstructor 这个对象么,只有logConstructor == null 才回去run线程
 * 所以说mybatis 默认按照顺序去加载的
 *
 * <setting name="logImpl" value="LOG4J"/>  可以去配置自定义也就是static 中的第一个
 *
 */
package org.apache.ibatis.logging;

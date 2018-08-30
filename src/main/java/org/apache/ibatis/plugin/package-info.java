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
 * Base package for handing plugins.
 * 插件包,一些插件的操作
 *
 * mybatis想要针对连接数据库做一些额外的操作就需要在某些类上做控制
 * 就是用代理模式,搭配拦截器的操作
 * 暴露到外层调用方的就是Interceptor这个接口
 * 这种单独的包都是比较通用的
 *
 * 拦截器栈,通过包装各个拦截器来生成组合的代理对象
 *
 * 但是什么类型的才代理呢,就是Signature 中 type 和 method,args对上的 method才进行拦截处理
 * 以及 拦截处理Plugin 将元代理对象封装到Invocation中
 */
package org.apache.ibatis.plugin;

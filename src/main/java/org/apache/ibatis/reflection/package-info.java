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
 * Reflection utils.
 * 反射包,应该都是和反射有关的操作把,通过反射去进行操作
 * Mybatis中应该也经常使用反射来给修改类
 *
 * 包内有四个子包
 * wrapper      包装包
 * property     属性操作包
 * invoker      执行操作
 * factory      工厂包
 *              数据库->java对象息息相关把
 *
 *
 * 反射包下,主要是围绕对象MetaObject 然后进行反射操作,会解析词法
 * MetaObject   -> ObjectWrapper
 *              -> ObjectFactory
 *              -> ObjectWrapperFactory
 *              -> ReflectorFactory
 *                  -> 这就可以做关于反射,解析string,创建对象,通过对象属性copy,拿到对象的所有信息.
 *                  -> 执行这个对象的方法,仅仅需要一个全路径的类名通过反射机制就可以搞定！
 */
package org.apache.ibatis.reflection;

/**
 *    Copyright 2009-2015 the original author or authors.
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
 * Contains all the annotation that are used in mapper interfaces
 * 注解包,修饰接口的注解
 * 这里面包含了很多注解,用来替代xml进行使用的,这些注解大部分用于接口中,方便生成需要的代理类
 *
 * Delete,Insert,Select,Update                                  这四个注解直接拼 sql
 * DeleteProvide,InsertProvide,SelectProvide,SelectProvide      这四个注解通过 类.方法   返回String 来辅助拼sql
 *
 * http://www.mybatis.org/mybatis-3/java-api.html
 * 官方文档来帮助你把,哈哈
 */
package org.apache.ibatis.annotations;


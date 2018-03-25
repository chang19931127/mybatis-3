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
 * Utilities to read resources.
 * 资源包,对一些资源进行管理,主要就是读入文件io把
 * VFS 一个工具类把,可以找到应用程序 path下的所有资源！
 * ResolverUtil这个工具类使用来扫包了
 *
 * 真要我们拿到资源其他的操作就无所谓了！通过类加载器load资源
 */
package org.apache.ibatis.io;

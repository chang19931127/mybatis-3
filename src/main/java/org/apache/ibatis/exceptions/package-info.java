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
 * Base package for exceptions.
 * 异常包,自定义异常
 * 自定义异常,里面有一个简单工厂模式
 * 走RuntimeException     基本上RuntimeException就是程序发生的错误我们无法解决
 *                      checkedException我们可以catch然后解决异常 例如 网络连接重试 等等
 */
package org.apache.ibatis.exceptions;

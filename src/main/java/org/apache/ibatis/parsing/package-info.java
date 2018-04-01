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
 * Parsing utils
 * 解析包,解析某些内容
 *
 * 关于Token 的操作
 * 感觉业内不叫通用的就是${}    #{}    剩下就是xml 角标了
 * 其实都是通过解析器来完成的,是不是这种方式也可以使用Json的替换呢,剩下的只是性能问题了
 *
 *      GenericTokenParse 通用的Token解析
 *      PropertyParse 通过GenericTokenParse 来解析${} 的被Properties中的进行替换
 *
 *      XNode和XPathParse 都是针对XPath 的进行解析了
 */
package org.apache.ibatis.parsing;

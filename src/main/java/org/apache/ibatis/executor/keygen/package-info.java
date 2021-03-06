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
 * Contains the key generators
 * 主要就是 key 的生成了 三种实现
 * NoKeyGenerator 不生成key 完全就是空实现
 * Jdbc3KeyGenerator  执行出来结果后 处理 key
 * SelectKeyGenerator 通过xml中的selectKey来生成    执行前后都 都可以调用
 */
package org.apache.ibatis.executor.keygen;

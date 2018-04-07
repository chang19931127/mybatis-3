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
 * Contains cache decorators
 * 缓存装饰,这样就有五花八门的功能了哈哈
 * 好好来学设计模式啊
 * BlockingCache 以key的维度来对缓存进行锁阻塞程序访问,性能肯定不好
 * FifoCache 以key维度通过Deque来存储key进行remove
 * LruCache 以key维度,通过LinkedHashMap的特性来进行Eldest remove,最久未使用remove
 * LoggingCache 提供了日志功能每次get 都会去打印相关操作
 * SerializedCache 针对value是可序列化的进行序列化缓存
 * ScheduledCache 针对时间间隔,通过调用缓存api 去进行清除缓存
 * SynchronizedCache 针对所有缓存的api 都进行synchronized关键字修饰通过jvm去管理同步
 *
 * 软引用和弱引用还需要再看啊       之前有一个例子如果是虚引用就可以成功但是变成软引用和弱引用也会失败
 *
 * TransactionalCache 内部自己有一个容器用来暂存然后通过commit和rollback来决定是否到刷到缓存中
 */
package org.apache.ibatis.cache.decorators;

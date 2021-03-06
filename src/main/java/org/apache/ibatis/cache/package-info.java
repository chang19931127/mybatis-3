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
 * Base package for caching stuff
 * 缓存包,缓存一些查询把
 * 数据库查询出来某些操作，可以将其缓存起来,暂时想这么理解
 * 缓存实例,然后可以get put remove 等操作
 *
 * CacheKey 缓存ArrayList  这样这个key很丰富
 * Cache 缓存对象
 * TransactionalCache    增加了事务能力
 *
 * 最终都被TransactionalCacheManager进行管理
 */
package org.apache.ibatis.cache;

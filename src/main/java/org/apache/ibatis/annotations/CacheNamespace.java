/**
 * Copyright 2009-2016 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 * 该命名空间内允许使用 内置缓存 可以保证写操作 线程安全
 * 通过这个注解 来进行 缓存操作
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CacheNamespace {
	// 缓存
	Class<? extends org.apache.ibatis.cache.Cache> implementation() default PerpetualCache.class;
	// 淘汰策略
	Class<? extends org.apache.ibatis.cache.Cache> eviction() default LruCache.class;
	// 过期时间
	long flushInterval() default 0;
	// 缓存大小
	int size() default 1024;
	// 可以读写
	boolean readWrite() default true;
	// 是否阻塞
	boolean blocking() default false;

	/**
	 * Property values for a implementation object.
	 * 给 Namespace 添加 key value
	 * @since 3.4.2
	 */
	Property[] properties() default {};

}

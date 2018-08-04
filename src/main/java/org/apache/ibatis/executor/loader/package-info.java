/**
 * Copyright 2009-2015 the original author or authors.
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
 * <p>
 * Base package for loading results into beans
 */
/**
 * Base package for loading results into beans
 *
 * 基本包，加载结果集到 bean中
 *
 * 这个包里面主要就是三个类
 * AbstractEnhanceDeserializationProxy
 * AbstractSerialStateHold
 * ProxyFactory
 *
 *
 * StateHold 通过 代理工厂生成代理
 * 代理工程生成代理
 * 为啥不使用动态代理，这里都是继承类把
 * 主要功能就是 生成一个 不同操作的类，并且附上属性把
 *
 * 通过这个包可以了解到 相关 cglib 和 javasisit 的 api 使用
 */
package org.apache.ibatis.executor.loader;

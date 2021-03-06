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
package org.apache.ibatis.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;

/**
 * @author Clinton Begin
 * 类型别名注册
 * 原生类型 _short
 */
public class TypeAliasRegistry {

	/**
	 * 都在这个Map中 别名 类型
	 */
	private final Map<String, Class<?>> TYPE_ALIASES = new HashMap<String, Class<?>>();

	public TypeAliasRegistry() {
		registerAlias("string", String.class);

		registerAlias("byte", Byte.class);
		registerAlias("long", Long.class);
		registerAlias("short", Short.class);
		registerAlias("int", Integer.class);
		registerAlias("integer", Integer.class);
		registerAlias("double", Double.class);
		registerAlias("float", Float.class);
		registerAlias("boolean", Boolean.class);

		registerAlias("byte[]", Byte[].class);
		registerAlias("long[]", Long[].class);
		registerAlias("short[]", Short[].class);
		registerAlias("int[]", Integer[].class);
		registerAlias("integer[]", Integer[].class);
		registerAlias("double[]", Double[].class);
		registerAlias("float[]", Float[].class);
		registerAlias("boolean[]", Boolean[].class);

		registerAlias("_byte", byte.class);
		registerAlias("_long", long.class);
		registerAlias("_short", short.class);
		registerAlias("_int", int.class);
		registerAlias("_integer", int.class);
		registerAlias("_double", double.class);
		registerAlias("_float", float.class);
		registerAlias("_boolean", boolean.class);

		registerAlias("_byte[]", byte[].class);
		registerAlias("_long[]", long[].class);
		registerAlias("_short[]", short[].class);
		registerAlias("_int[]", int[].class);
		registerAlias("_integer[]", int[].class);
		registerAlias("_double[]", double[].class);
		registerAlias("_float[]", float[].class);
		registerAlias("_boolean[]", boolean[].class);

		registerAlias("date", Date.class);
		registerAlias("decimal", BigDecimal.class);
		registerAlias("bigdecimal", BigDecimal.class);
		registerAlias("biginteger", BigInteger.class);
		registerAlias("object", Object.class);

		registerAlias("date[]", Date[].class);
		registerAlias("decimal[]", BigDecimal[].class);
		registerAlias("bigdecimal[]", BigDecimal[].class);
		registerAlias("biginteger[]", BigInteger[].class);
		registerAlias("object[]", Object[].class);

		registerAlias("map", Map.class);
		registerAlias("hashmap", HashMap.class);
		registerAlias("list", List.class);
		registerAlias("arraylist", ArrayList.class);
		registerAlias("collection", Collection.class);
		registerAlias("iterator", Iterator.class);

		registerAlias("ResultSet", ResultSet.class);
	}

	@SuppressWarnings("unchecked")
	// throws class cast exception as well if types cannot be assigned
	/**
	 * 通过别名获取类型
	 */
	public <T> Class<T> resolveAlias(String string) {
		try {
			if (string == null) {
				return null;
			}
			// issue #748
			String key = string.toLowerCase(Locale.ENGLISH);
			Class<T> value;
			if (TYPE_ALIASES.containsKey(key)) {
				value = (Class<T>) TYPE_ALIASES.get(key);
			} else {
				value = (Class<T>) Resources.classForName(string);
			}
			return value;
		} catch (ClassNotFoundException e) {
			throw new TypeException("Could not resolve type alias '" + string + "'.  Cause: " + e, e);
		}
	}



	// registerAlias 有好几个重载方法

	/**
	 * 直接指定包名,将包名下面的是java对象的全部注解成    类名的simple - 类
	 * @param packageName
	 */
	public void registerAliases(String packageName) {
		registerAliases(packageName, Object.class);
	}

	/**
	 * 找到包路径下面的     java对象
	 * 然后保证这些对象不是匿名类,接口,成员类
	 * 然后直接注册成 简单名 - 类
	 * @param packageName
	 * @param superType
	 */
	public void registerAliases(String packageName, Class<?> superType) {
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
		resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
		Set<Class<? extends Class<?>>> typeSet = resolverUtil.getClasses();
		for (Class<?> type : typeSet) {
			// Ignore inner classes and interfaces (including package-info.java)
			// Skip also inner classes. See issue #6
			if (!type.isAnonymousClass() && !type.isInterface() && !type.isMemberClass()) {
				registerAlias(type);
			}
		}
	}

	/**
	 * 字节码对象 和 字节码对象的simpleName
	 * @param type
	 */
	public void registerAlias(Class<?> type) {
		String alias = type.getSimpleName();
		Alias aliasAnnotation = type.getAnnotation(Alias.class);
		if (aliasAnnotation != null) {
			alias = aliasAnnotation.value();
		}
		registerAlias(alias, type);
	}

	/**
	 * 别名 - 字节码对象
	 * @param alias
	 * @param value
	 */
	public void registerAlias(String alias, Class<?> value) {
		if (alias == null) {
			throw new TypeException("The parameter alias cannot be null");
		}
		// issue #748
		// key大写后校验
		String key = alias.toLowerCase(Locale.ENGLISH);
		if (TYPE_ALIASES.containsKey(key) && TYPE_ALIASES.get(key) != null && !TYPE_ALIASES.get(key).equals(value)) {
			throw new TypeException("The alias '" + alias + "' is already mapped to the value '" + TYPE_ALIASES.get(key).getName() + "'.");
		}
		// 存起来
		TYPE_ALIASES.put(key, value);
	}

	/**
	 * 别名 - 类全限定名
	 * @param alias
	 * @param value
	 */
	public void registerAlias(String alias, String value) {
		try {
			registerAlias(alias, Resources.classForName(value));
		} catch (ClassNotFoundException e) {
			throw new TypeException("Error registering type alias " + alias + " for " + value + ". Cause: " + e, e);
		}
	}

	/**
	 * @since 3.2.2
	 * 返回不可修改map
	 */
	public Map<String, Class<?>> getTypeAliases() {
		return Collections.unmodifiableMap(TYPE_ALIASES);
	}

}

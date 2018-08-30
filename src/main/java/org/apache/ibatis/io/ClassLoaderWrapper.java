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
package org.apache.ibatis.io;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

/**
 * A class to wrap access to multiple class loaders making them work as one
 * 类加载器的包装类,好处就是使用一个包装类就貌似使用了多个类加载器
 * 可以认为是门面模式么,封装了其他的操作
 *
 * 通过类加载器获得资源
 * 通过类加载器获得资源流
 * 通过类加载器获得URL
 *
 * 主要屏蔽使用的那种类加载器 主要是为了获得资源
 * @author Clinton Begin
 */
public class ClassLoaderWrapper {

	ClassLoader defaultClassLoader;
	ClassLoader systemClassLoader;

	ClassLoaderWrapper() {
		try {
			systemClassLoader = ClassLoader.getSystemClassLoader();
		} catch (SecurityException ignored) {
			// AccessControlException on Google App Engine
		}
	}

	/**
	 * Get a resource as a URL using the current class path
	 * 得到资源的URL形式
	 * @param resource - the resource to locate
	 * @return the resource or null
	 */
	public URL getResourceAsURL(String resource) {
		return getResourceAsURL(resource, getClassLoaders(null));
	}

	/**
	 * Get a resource from the classpath, starting with a specific class loader
	 * 得到资源的url形式 不同的重载方法
	 * @param resource    - the resource to find
	 * @param classLoader - the first classloader to try
	 * @return the stream or null
	 */
	public URL getResourceAsURL(String resource, ClassLoader classLoader) {
		return getResourceAsURL(resource, getClassLoaders(classLoader));
	}

	/**
	 * Get a resource from the classpath
	 * 通过resource 直接就获得输入流了
	 * @param resource - the resource to find
	 * @return the stream or null
	 */
	public InputStream getResourceAsStream(String resource) {
		return getResourceAsStream(resource, getClassLoaders(null));
	}

	/**
	 * Get a resource from the classpath, starting with a specific class loader
	 * 一样的重载方法
	 * @param resource    - the resource to find
	 * @param classLoader - the first class loader to try
	 * @return the stream or null
	 */
	public InputStream getResourceAsStream(String resource, ClassLoader classLoader) {
		return getResourceAsStream(resource, getClassLoaders(classLoader));
	}

	/**
	 * Find a class on the classpath (or die trying)
	 * 通过全限定名,获得Class对象
	 * @param name - the class to look for
	 * @return - the class
	 * @throws ClassNotFoundException Duh.
	 */
	public Class<?> classForName(String name) throws ClassNotFoundException {
		return classForName(name, getClassLoaders(null));
	}

	/**
	 * Find a class on the classpath, starting with a specific classloader (or die trying)
	 * 获得Class的重载方法
	 * @param name        - the class to look for
	 * @param classLoader - the first classloader to try
	 * @return - the class
	 * @throws ClassNotFoundException Duh.
	 */
	public Class<?> classForName(String name, ClassLoader classLoader) throws ClassNotFoundException {
		return classForName(name, getClassLoaders(classLoader));
	}

	/**
	 * Try to get a resource from a group of classloaders
	 * 通过类加载器来获得资源    并转化成流
	 * @param resource    - the resource to get
	 * @param classLoader - the classloaders to examine
	 * @return the resource or null
	 */
	InputStream getResourceAsStream(String resource, ClassLoader[] classLoader) {
		// 循环遍历,谁加载了就返回
		for (ClassLoader cl : classLoader) {
			if (null != cl) {

				// try to find the resource as passed
				InputStream returnValue = cl.getResourceAsStream(resource);

				// now, some class loaders want this leading "/", so we'll add it and try again if we didn't find the resource
				if (null == returnValue) {
					returnValue = cl.getResourceAsStream("/" + resource);
				}

				if (null != returnValue) {
					return returnValue;
				}
			}
		}
		return null;
	}

	/**
	 * Get a resource as a URL using the current class path
	 * 得到这个资源的URL形式
	 * @param resource    - the resource to locate
	 * @param classLoader - the class loaders to examine
	 * @return the resource or null
	 */
	URL getResourceAsURL(String resource, ClassLoader[] classLoader) {

		URL url;

		for (ClassLoader cl : classLoader) {

			if (null != cl) {

				// look for the resource as passed in...
				url = cl.getResource(resource);

				// ...but some class loaders want this leading "/", so we'll add it
				// and try again if we didn't find the resource
				if (null == url) {
					url = cl.getResource("/" + resource);
				}

				// "It's always in the last place I look for it!"
				// ... because only an idiot would keep looking for it after finding it, so stop looking already.
				if (null != url) {
					return url;
				}

			}

		}

		// didn't find it anywhere.
		return null;

	}

	/**
	 * Attempt to load a class from a group of classloaders
	 *
	 * @param name        - the class to load
	 * @param classLoader - the group of classloaders to examine
	 * @return the class
	 * @throws ClassNotFoundException - Remember the wisdom of Judge Smails: Well, the world needs ditch diggers, too.
	 */
	Class<?> classForName(String name, ClassLoader[] classLoader) throws ClassNotFoundException {
		// 也是循环遍历,然后直接forName来加载
		for (ClassLoader cl : classLoader) {

			if (null != cl) {

				try {

					Class<?> c = Class.forName(name, true, cl);

					if (null != c) {
						return c;
					}

				} catch (ClassNotFoundException e) {
					// we'll ignore this until all classloaders fail to locate the class
				}

			}

		}

		throw new ClassNotFoundException("Cannot find class: " + name);

	}

	/**
	 * 得到所有的类加载器
	 * 传入的类加载器,默认的类加载器,当前上下文类加载器,该类的类加载器,系统类加载器
	 * @param classLoader
	 * @return
	 */
	ClassLoader[] getClassLoaders(ClassLoader classLoader) {
		return new ClassLoader[]{
				classLoader,
				defaultClassLoader,
				Thread.currentThread().getContextClassLoader(),
				getClass().getClassLoader(),
				systemClassLoader};
	}

	public static void main(String[] args) {
		System.out.println(Arrays.toString(new ClassLoaderWrapper().getClassLoaders(null)));
	}

}

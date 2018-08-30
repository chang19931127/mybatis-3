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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * A class to simplify access to resources through the classloader.
 * 一个通过类加载器简单的访问资源
 * @author Clinton Begin
 */
public class Resources {

	/**
	 * 内部的类加载器都交给CLassLoaderWrapper
	 */
	private static ClassLoaderWrapper classLoaderWrapper = new ClassLoaderWrapper();

	/*
	 * Charset to use when calling getResourceAsReader.
	 * null means use the system default.
	 */
	private static Charset charset;

	Resources() {
	}

	/**
	 * Returns the default classloader (may be null).
	 * 返回默认的类加载器
	 * @return The default classloader
	 */
	public static ClassLoader getDefaultClassLoader() {
		return classLoaderWrapper.defaultClassLoader;
	}

	/**
	 * Sets the default classloader
	 * 设置默认的类加载器
	 * @param defaultClassLoader - the new default ClassLoader
	 */
	public static void setDefaultClassLoader(ClassLoader defaultClassLoader) {
		classLoaderWrapper.defaultClassLoader = defaultClassLoader;
	}

	/**
	 * Returns the URL of the resource on the classpath
	 * 返回资源的URL形式
	 * @param resource The resource to find
	 * @return The resource
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static URL getResourceURL(String resource) throws IOException {
		// issue #625
		return getResourceURL(null, resource);
	}

	/**
	 * Returns the URL of the resource on the classpath
	 * 返回URL的重载形式
	 * @param loader   The classloader used to fetch the resource
	 * @param resource The resource to find
	 * @return The resource
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static URL getResourceURL(ClassLoader loader, String resource) throws IOException {
		URL url = classLoaderWrapper.getResourceAsURL(resource, loader);
		if (url == null) {
			throw new IOException("Could not find resource " + resource);
		}
		return url;
	}

	/**
	 * Returns a resource on the classpath as a Stream object
	 * 返回输入流
	 * @param resource The resource to find
	 * @return The resource
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static InputStream getResourceAsStream(String resource) throws IOException {
		return getResourceAsStream(null, resource);
	}

	/**
	 * Returns a resource on the classpath as a Stream object
	 * 返回资源的输入流
	 * @param loader   The classloader used to fetch the resource
	 * @param resource The resource to find
	 * @return The resource
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static InputStream getResourceAsStream(ClassLoader loader, String resource) throws IOException {
		InputStream in = classLoaderWrapper.getResourceAsStream(resource, loader);
		if (in == null) {
			throw new IOException("Could not find resource " + resource);
		}
		return in;
	}

	/**
	 * Returns a resource on the classpath as a Properties object
	 * 返回Properties
	 * @param resource The resource to find
	 * @return The resource
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static Properties getResourceAsProperties(String resource) throws IOException {
		Properties props = new Properties();
		InputStream in = getResourceAsStream(resource);
		props.load(in);
		in.close();
		return props;
	}

	/**
	 * Returns a resource on the classpath as a Properties object
	 * 指定类加载器取properties文件
	 * @param loader   The classloader used to fetch the resource
	 * @param resource The resource to find
	 * @return The resource
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static Properties getResourceAsProperties(ClassLoader loader, String resource) throws IOException {
		Properties props = new Properties();
		InputStream in = getResourceAsStream(loader, resource);
		props.load(in);
		in.close();
		return props;
	}

	/**
	 * Returns a resource on the classpath as a Reader object
	 * Read流
	 * @param resource The resource to find
	 * @return The resource
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static Reader getResourceAsReader(String resource) throws IOException {
		Reader reader;
		if (charset == null) {
			//直接字节流然后转化
			reader = new InputStreamReader(getResourceAsStream(resource));
		} else {
			reader = new InputStreamReader(getResourceAsStream(resource), charset);
		}
		return reader;
	}

	/**
	 * Returns a resource on the classpath as a Reader object
	 * 重载形式
	 * @param loader   The classloader used to fetch the resource
	 * @param resource The resource to find
	 * @return The resource
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static Reader getResourceAsReader(ClassLoader loader, String resource) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getResourceAsStream(loader, resource));
		} else {
			reader = new InputStreamReader(getResourceAsStream(loader, resource), charset);
		}
		return reader;
	}

	/**
	 * Returns a resource on the classpath as a File object
	 * 传入的String 直接返回File对象
	 * @param resource The resource to find
	 * @return The resource
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static File getResourceAsFile(String resource) throws IOException {
		return new File(getResourceURL(resource).getFile());
	}

	/**
	 * Returns a resource on the classpath as a File object
	 * 类加载然后返回FIle对象
	 * @param loader   - the classloader used to fetch the resource
	 * @param resource - the resource to find
	 * @return The resource
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static File getResourceAsFile(ClassLoader loader, String resource) throws IOException {
		return new File(getResourceURL(loader, resource).getFile());
	}

	/**
	 * Gets a URL as an input stream
	 * 返回流对象
	 * @param urlString - the URL to get
	 * @return An input stream with the data from the URL
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static InputStream getUrlAsStream(String urlString) throws IOException {
		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();
		return conn.getInputStream();
	}

	/**
	 * Gets a URL as a Reader
	 * 流对象
	 * @param urlString - the URL to get
	 * @return A Reader with the data from the URL
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static Reader getUrlAsReader(String urlString) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getUrlAsStream(urlString));
		} else {
			reader = new InputStreamReader(getUrlAsStream(urlString), charset);
		}
		return reader;
	}

	/**
	 * Gets a URL as a Properties object
	 * Properties对象
	 * @param urlString - the URL to get
	 * @return A Properties object with the data from the URL
	 * @throws java.io.IOException If the resource cannot be found or read
	 */
	public static Properties getUrlAsProperties(String urlString) throws IOException {
		Properties props = new Properties();
		InputStream in = getUrlAsStream(urlString);
		props.load(in);
		in.close();
		return props;
	}

	/**
	 * Loads a class
	 * 直接加载类
	 * @param className - the class to fetch
	 * @return The loaded class
	 * @throws ClassNotFoundException If the class cannot be found (duh!)
	 */
	public static Class<?> classForName(String className) throws ClassNotFoundException {
		return classLoaderWrapper.classForName(className);
	}

	public static Charset getCharset() {
		return charset;
	}

	public static void setCharset(Charset charset) {
		Resources.charset = charset;
	}

	public static void main(String[] args) throws IOException {
		String s = "Resources";
		System.out.println(Resources.getResourceURL(s));
	}
}

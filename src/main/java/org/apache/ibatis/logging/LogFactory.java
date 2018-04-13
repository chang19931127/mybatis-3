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
package org.apache.ibatis.logging;

import java.lang.reflect.Constructor;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * 这个类很重要啊,通过工厂模式来帮助我们构建Log的实现类
 * 这里首先探入,需要去classPath 查找那个类存在就实例化
 *
 * static 代码块回去进行操作
 */
public final class LogFactory {

	/**
	 * Marker to be used by logging implementations that support markers
	 */
	public static final String MARKER = "MYBATIS";

	/**
	 * 构造器啊
	 */
	private static Constructor<? extends Log> logConstructor;

	static {

		// 通过静态代码块
		// 配合线程操作
		// 以及传入自己的封装类
		// 这么多个线程如set logConstructor 我们最后使用的是哪一个啊

		tryImplementation(new Runnable() {
			@Override
			public void run() {
				useSlf4jLogging();
			}
		});
		tryImplementation(new Runnable() {
			@Override
			public void run() {
				useCommonsLogging();
			}
		});
		tryImplementation(new Runnable() {
			@Override
			public void run() {
				useLog4J2Logging();
			}
		});
		tryImplementation(new Runnable() {
			@Override
			public void run() {
				useLog4JLogging();
			}
		});
		tryImplementation(new Runnable() {
			@Override
			public void run() {
				useJdkLogging();
			}
		});
		tryImplementation(new Runnable() {
			@Override
			public void run() {
				useNoLogging();
			}
		});
	}

	private LogFactory() {
		// disable construction
	}

	public static Log getLog(Class<?> aClass) {
		return getLog(aClass.getName());
	}

	public static Log getLog(String logger) {
		try {
			// 直接反射调用获得Log类型传入类名
			return logConstructor.newInstance(logger);
		} catch (Throwable t) {
			throw new LogException("Error creating logger for logger " + logger + ".  Cause: " + t, t);
		}
	}

	/**
	 * 可以通过程序调用这个方法,然后这个类才开始,但是logConstructor已经有值了
	 * <setting name="logImpl" value="LOG4J"/>   就是直接调用这个方法
	 * @param clazz
	 */
	public static synchronized void useCustomLogging(Class<? extends Log> clazz) {
		setImplementation(clazz);
	}

	/**
	 * 同步方法 使用Slf4j的日志
	 */
	public static synchronized void useSlf4jLogging() {
		setImplementation(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
	}

	public static synchronized void useCommonsLogging() {
		setImplementation(org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl.class);
	}

	public static synchronized void useLog4JLogging() {
		setImplementation(org.apache.ibatis.logging.log4j.Log4jImpl.class);
	}

	public static synchronized void useLog4J2Logging() {
		setImplementation(org.apache.ibatis.logging.log4j2.Log4j2Impl.class);
	}

	public static synchronized void useJdkLogging() {
		setImplementation(org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl.class);
	}

	public static synchronized void useStdOutLogging() {
		setImplementation(org.apache.ibatis.logging.stdout.StdOutImpl.class);
	}

	public static synchronized void useNoLogging() {
		setImplementation(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
	}

	/**
	 * 抽出方法,被其他地方公用
	 * @param runnable
	 */
	private static void tryImplementation(Runnable runnable) {
		// 会进行null判断只要不是null 就不run了
		if (logConstructor == null) {
			try {
				// 会不会运行的时候都找不到Jar就报错了,用户给那个jar就用那个
				runnable.run();
			} catch (Throwable t) {
				// ignore
			}
		}
	}

	/**
	 * 抽取公共方法,将传入的Class 通过反射传递给logConstructor对象
	 * @param implClass
	 */
	private static void setImplementation(Class<? extends Log> implClass) {
		try {
			// 统一反射 String参数的构造器,然后
			Constructor<? extends Log> candidate = implClass.getConstructor(String.class);
			// 先实例化,打印个日志
			Log log = candidate.newInstance(LogFactory.class.getName());
			if (log.isDebugEnabled()) {
				log.debug("Logging initialized using '" + implClass + "' adapter.");
			}
			logConstructor = candidate;
		} catch (Throwable t) {
			throw new LogException("Error setting Log implementation.  Cause: " + t, t);
		}
	}


	public static void main(String[] args) {
		System.out.println(new TestStatic());
		useStdOutLogging();
		LogFactory.getLog(Class.class).debug("开心");
		useJdkLogging();
		LogFactory.getLog(Class.class).debug("开心");
	}

}

class TestStatic{
	static{
		new Thread(() -> {
			test1();
		}).start();
		new Thread(() -> {
			test2();
		}).start();
		new Thread(() -> {
			test3();
		}).start();
	}

	private static synchronized void test1(){
		System.out.println("test1");
	}
	private static synchronized void test2(){
		System.out.println("test2");
	}
	private static synchronized void test3(){
		System.out.println("test3");
	}
}
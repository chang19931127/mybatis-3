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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Properties;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * @author Clinton Begin
 * 这个类过时了,字面就是外部资源
 * 一个copy资源使用nio进行
 * 一个就是通过properties 获得一个属性,基本就是封装底层方法,然后方便使用
 */
@Deprecated
public class ExternalResources {

	private static final Log log = LogFactory.getLog(ExternalResources.class);

	private ExternalResources() {
		// do nothing
	}

	/**
	 * 就是 复制文件了
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void copyExternalResource(File sourceFile, File destFile) throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			closeQuietly(source);
			closeQuietly(destination);
		}

	}

	private static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				// do nothing, close quietly
			}
		}
	}

	/**
	 * 通过一个 templatePath拿到一个properties
	 * 然后在properties去拿到属性,
	 * 但是每次都重新load    性能可观啊
	 * @param templatePath
	 * @param templateProperty
	 * @return
	 * @throws FileNotFoundException
	 */
	public static String getConfiguredTemplate(String templatePath, String templateProperty) throws FileNotFoundException {
		String templateName = "";
		Properties migrationProperties = new Properties();

		try {
			migrationProperties.load(new FileInputStream(templatePath));
			templateName = migrationProperties.getProperty(templateProperty);
		} catch (FileNotFoundException e) {
			throw e;
		} catch (Exception e) {
			log.error("", e);
		}

		return templateName;
	}

}

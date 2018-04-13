/**
 * Copyright 2009-2017 the original author or authors.
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
package org.apache.ibatis.jdbc;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Clinton Begin
 * SQL脚本语言执行类
 */
public class ScriptRunner {

	/**
	 * 获取当前系统的换行符
	 */
	private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

	/**
	 * 分好
	 */
	private static final String DEFAULT_DELIMITER = ";";

	/**
	 * 有必要了解以下这个正则     MySQL的注释有关
	 */
	private static final Pattern DELIMITER_PATTERN = Pattern.compile("^\\s*((--)|(//))?\\s*(//)?\\s*@DELIMITER\\s+([^\\s]+)", Pattern.CASE_INSENSITIVE);

	/**
	 * 数据库连接对象
	 */
	private final Connection connection;
	/**
	 * 出错停止
	 */
	private boolean stopOnError;
	private boolean throwWarning;
	/**
	 * 是否自动提交
	 */
	private boolean autoCommit;
	/**
	 * 是否发送完整的SQL基本
	 * 发现没   boolean  不用is 来定义变量
	 */
	private boolean sendFullScript;
	/**
	 * 移除CR \r\n ->\n
	 */
	private boolean removeCRs;
	private boolean escapeProcessing = true;

	private PrintWriter logWriter = new PrintWriter(System.out);
	private PrintWriter errorLogWriter = new PrintWriter(System.err);

	private String delimiter = DEFAULT_DELIMITER;

	// 是否整行
	private boolean fullLineDelimiter;

	public ScriptRunner(Connection connection) {
		// 构造传入连接对象
		this.connection = connection;
	}

	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	public void setThrowWarning(boolean throwWarning) {
		this.throwWarning = throwWarning;
	}

	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

	public void setSendFullScript(boolean sendFullScript) {
		this.sendFullScript = sendFullScript;
	}

	public void setRemoveCRs(boolean removeCRs) {
		this.removeCRs = removeCRs;
	}

	/**
	 * @since 3.1.1
	 */
	public void setEscapeProcessing(boolean escapeProcessing) {
		this.escapeProcessing = escapeProcessing;
	}

	public void setLogWriter(PrintWriter logWriter) {
		this.logWriter = logWriter;
	}

	public void setErrorLogWriter(PrintWriter errorLogWriter) {
		this.errorLogWriter = errorLogWriter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public void setFullLineDelimiter(boolean fullLineDelimiter) {
		this.fullLineDelimiter = fullLineDelimiter;
	}

	/**
	 * 执行脚本,前提条件是自动提交事务,否则无效
	 * @param reader
	 */
	public void runScript(Reader reader) {
		// 设置自动提交和 程序中的autoCommit一致
		// 如果是非自动提交,就要回滚,如果是自动提交正常
		// 因为这一波逻辑没有提交事务的逻辑
		setAutoCommit();

		try {
			if (sendFullScript) {
				// 执行脚本 存储过程
				executeFullScript(reader);
			} else {
				// 执行语句
				executeLineByLine(reader);
			}
		} finally {
			// 都已经提交了
			rollbackConnection();
		}
	}

	/**
	 * 执行全部脚本
	 * @param reader
	 */
	private void executeFullScript(Reader reader) {
		StringBuilder script = new StringBuilder();
		try {
			BufferedReader lineReader = new BufferedReader(reader);
			String line;
			// BufferedReader 一行一行读  然后追加 换行符
			while ((line = lineReader.readLine()) != null) {
				script.append(line);
				script.append(LINE_SEPARATOR);
			}
			String command = script.toString();
			println(command);
			// 执行sql
			executeStatement(command);
			// 并提交事物
			commitConnection();
		} catch (Exception e) {
			String message = "Error executing: " + script + ".  Cause: " + e;
			printlnError(message);
			throw new RuntimeSqlException(message, e);
		}
	}

	/**
	 * 执行一行
	 * @param reader
	 */
	private void executeLineByLine(Reader reader) {
		StringBuilder command = new StringBuilder();
		try {
			BufferedReader lineReader = new BufferedReader(reader);
			String line;
			while ((line = lineReader.readLine()) != null) {
				// 处理语句做加工
				handleLine(command, line);
			}
			// 提交连接
			commitConnection();
			// 检查是否少分号
			checkForMissingLineTerminator(command);
		} catch (Exception e) {
			String message = "Error executing: " + command + ".  Cause: " + e;
			printlnError(message);
			throw new RuntimeSqlException(message, e);
		}
	}

	/**
	 * 关闭连接
	 */
	public void closeConnection() {
		try {
			connection.close();
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * 设置自动提交 和autoCommit一致
	 */
	private void setAutoCommit() {
		try {
			if (autoCommit != connection.getAutoCommit()) {
				connection.setAutoCommit(autoCommit);
			}
		} catch (Throwable t) {
			throw new RuntimeSqlException("Could not set AutoCommit to " + autoCommit + ". Cause: " + t, t);
		}
	}

	/**
	 * 如果不是自动提交
	 * 就帮助提交
	 */
	private void commitConnection() {
		try {
			if (!connection.getAutoCommit()) {
				connection.commit();
			}
		} catch (Throwable t) {
			throw new RuntimeSqlException("Could not commit transaction. Cause: " + t, t);
		}
	}

	/**
	 * 如果不是自动提交就回滚
	 */
	private void rollbackConnection() {
		try {
			if (!connection.getAutoCommit()) {
				connection.rollback();
			}
		} catch (Throwable t) {
			// ignore
		}
	}

	/**
	 * 检查是不是没完成 有问题
	 * @param command
	 */
	private void checkForMissingLineTerminator(StringBuilder command) {
		if (command != null && command.toString().trim().length() > 0) {
			throw new RuntimeSqlException("Line missing end-of-line terminator (" + delimiter + ") => " + command);
		}
	}

	/**
	 * 处理MySQL语句
	 * 注释部分
	 * 带分号的语句部分
	 * @param command
	 * @param line
	 * @throws SQLException
	 */
	private void handleLine(StringBuilder command, String line) throws SQLException {
		String trimmedLine = line.trim();
		// MySQL 注释 -- //
		if (lineIsComment(trimmedLine)) {
			Matcher matcher = DELIMITER_PATTERN.matcher(trimmedLine);
			if (matcher.find()) {
				delimiter = matcher.group(5);
			}
			// 打印注释有效信息
			println(trimmedLine);
		} else if (commandReadyToExecute(trimmedLine)) {
			// 以分号来隔断
			command.append(line.substring(0, line.lastIndexOf(delimiter)));
			command.append(LINE_SEPARATOR);
			println(command);
			// 执行一个分号语句
			executeStatement(command.toString());
			command.setLength(0);
		} else if (trimmedLine.length() > 0) {
			// 换行
			command.append(line);
			command.append(LINE_SEPARATOR);
		}
	}

	private boolean lineIsComment(String trimmedLine) {
		return trimmedLine.startsWith("//") || trimmedLine.startsWith("--");
	}

	private boolean commandReadyToExecute(String trimmedLine) {
		// issue #561 remove anything after the delimiter
		// 不正行,包含分行,整行是分号
		return !fullLineDelimiter && trimmedLine.contains(delimiter) || fullLineDelimiter && trimmedLine.equals(delimiter);
	}

	/**
	 * 执行语句的方法
	 * @param command
	 * @throws SQLException
	 */
	private void executeStatement(String command) throws SQLException {
		boolean hasResults = false;
		Statement statement = connection.createStatement();
		// 过滤
		statement.setEscapeProcessing(escapeProcessing);
		String sql = command;
		if (removeCRs) {
			sql = sql.replaceAll("\r\n", "\n");
		}
		if (stopOnError) {
			// 直接执行sql
			hasResults = statement.execute(sql);
			if (throwWarning) {
				// In Oracle, CRATE PROCEDURE, FUNCTION, etc. returns warning
				// instead of throwing exception if there is compilation error.
				SQLWarning warning = statement.getWarnings();
				if (warning != null) {
					throw warning;
				}
			}
		} else {
			try {
				hasResults = statement.execute(sql);
			} catch (SQLException e) {
				String message = "Error executing: " + command + ".  Cause: " + e;
				printlnError(message);
			}
		}
		printResults(statement, hasResults);
		try {
			// 一定要关闭
			statement.close();
		} catch (Exception e) {
			// Ignore to workaround a bug in some connection pools
		}
	}

	/**
	 * 打印出来结果  执行数据的结果
	 * @param statement
	 * @param hasResults
	 */
	private void printResults(Statement statement, boolean hasResults) {
		try {
			if (hasResults) {
				ResultSet rs = statement.getResultSet();
				if (rs != null) {
					// 通过数据库元信息来整的
					ResultSetMetaData md = rs.getMetaData();
					int cols = md.getColumnCount();
					for (int i = 0; i < cols; i++) {
						String name = md.getColumnLabel(i + 1);
						print(name + "\t");
					}
					println("");
					while (rs.next()) {
						for (int i = 0; i < cols; i++) {
							String value = rs.getString(i + 1);
							print(value + "\t");
						}
						println("");
					}
				}
			}
		} catch (SQLException e) {
			printlnError("Error printing results: " + e.getMessage());
		}
	}

	private void print(Object o) {
		if (logWriter != null) {
			logWriter.print(o);
			logWriter.flush();
		}
	}

	/**
	 * 记录出来数据
	 * @param o
	 */
	private void println(Object o) {
		if (logWriter != null) {
			logWriter.println(o);
			logWriter.flush();
		}
	}

	/**
	 * error也记录出来
	 * @param o
	 */
	private void printlnError(Object o) {
		if (errorLogWriter != null) {
			errorLogWriter.println(o);
			errorLogWriter.flush();
		}
	}

}

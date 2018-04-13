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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Clinton Begin
 * @author Jeff Butler
 * @author Adam Gent
 * @author Kazuki Shimizu
 *         <p>
 *         抽象SQL类把,这个类封装最基础的操作
 */
public abstract class AbstractSQL<T> {

	/**
	 * \n是换行
	 */
	private static final String AND = ") \nAND (";
	private static final String OR = ") \nOR (";

	/**
	 * 这个对象很牛
	 */
	private final SQLStatement sql = new SQLStatement();

	public abstract T getSelf();

	public T UPDATE(String table) {
		sql().statementType = SQLStatement.StatementType.UPDATE;
		sql().tables.add(table);
		return getSelf();
	}

	public T SET(String sets) {
		sql().sets.add(sets);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T SET(String... sets) {
		sql().sets.addAll(Arrays.asList(sets));
		return getSelf();
	}

	public T INSERT_INTO(String tableName) {
		sql().statementType = SQLStatement.StatementType.INSERT;
		sql().tables.add(tableName);
		return getSelf();
	}

	public T VALUES(String columns, String values) {
		sql().columns.add(columns);
		sql().values.add(values);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T INTO_COLUMNS(String... columns) {
		sql().columns.addAll(Arrays.asList(columns));
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T INTO_VALUES(String... values) {
		sql().values.addAll(Arrays.asList(values));
		return getSelf();
	}

	public T SELECT(String columns) {
		sql().statementType = SQLStatement.StatementType.SELECT;
		sql().select.add(columns);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T SELECT(String... columns) {
		sql().statementType = SQLStatement.StatementType.SELECT;
		sql().select.addAll(Arrays.asList(columns));
		return getSelf();
	}

	public T SELECT_DISTINCT(String columns) {
		sql().distinct = true;
		SELECT(columns);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T SELECT_DISTINCT(String... columns) {
		sql().distinct = true;
		SELECT(columns);
		return getSelf();
	}

	public T DELETE_FROM(String table) {
		sql().statementType = SQLStatement.StatementType.DELETE;
		sql().tables.add(table);
		return getSelf();
	}

	public T FROM(String table) {
		sql().tables.add(table);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T FROM(String... tables) {
		sql().tables.addAll(Arrays.asList(tables));
		return getSelf();
	}

	public T JOIN(String join) {
		sql().join.add(join);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T JOIN(String... joins) {
		sql().join.addAll(Arrays.asList(joins));
		return getSelf();
	}

	public T INNER_JOIN(String join) {
		sql().innerJoin.add(join);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T INNER_JOIN(String... joins) {
		sql().innerJoin.addAll(Arrays.asList(joins));
		return getSelf();
	}

	public T LEFT_OUTER_JOIN(String join) {
		sql().leftOuterJoin.add(join);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T LEFT_OUTER_JOIN(String... joins) {
		sql().leftOuterJoin.addAll(Arrays.asList(joins));
		return getSelf();
	}

	public T RIGHT_OUTER_JOIN(String join) {
		sql().rightOuterJoin.add(join);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T RIGHT_OUTER_JOIN(String... joins) {
		sql().rightOuterJoin.addAll(Arrays.asList(joins));
		return getSelf();
	}

	public T OUTER_JOIN(String join) {
		sql().outerJoin.add(join);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T OUTER_JOIN(String... joins) {
		sql().outerJoin.addAll(Arrays.asList(joins));
		return getSelf();
	}

	public T WHERE(String conditions) {
		sql().where.add(conditions);
		sql().lastList = sql().where;
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T WHERE(String... conditions) {
		sql().where.addAll(Arrays.asList(conditions));
		sql().lastList = sql().where;
		return getSelf();
	}

	public T OR() {
		sql().lastList.add(OR);
		return getSelf();
	}

	public T AND() {
		sql().lastList.add(AND);
		return getSelf();
	}

	public T GROUP_BY(String columns) {
		sql().groupBy.add(columns);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T GROUP_BY(String... columns) {
		sql().groupBy.addAll(Arrays.asList(columns));
		return getSelf();
	}

	public T HAVING(String conditions) {
		sql().having.add(conditions);
		sql().lastList = sql().having;
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T HAVING(String... conditions) {
		sql().having.addAll(Arrays.asList(conditions));
		sql().lastList = sql().having;
		return getSelf();
	}

	public T ORDER_BY(String columns) {
		sql().orderBy.add(columns);
		return getSelf();
	}

	/**
	 * @since 3.4.2
	 */
	public T ORDER_BY(String... columns) {
		sql().orderBy.addAll(Arrays.asList(columns));
		return getSelf();
	}

	private SQLStatement sql() {
		return sql;
	}

	public <A extends Appendable> A usingAppender(A a) {
		sql().sql(a);
		return a;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sql().sql(sb);
		return sb.toString();
	}

	/**
	 * 一个静态内部类
	 * 封装一层Appendable对象
	 * 添加一个是否为 null 的字段
	 */
	private static class SafeAppendable {
		private final Appendable a;
		private boolean empty = true;

		public SafeAppendable(Appendable a) {
			super();
			this.a = a;
		}

		public SafeAppendable append(CharSequence s) {
			try {
				if (empty && s.length() > 0) {
					empty = false;
				}
				a.append(s);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return this;
		}

		public boolean isEmpty() {
			return empty;
		}

	}

	/**
	 * 基本上所有的sql 语句都在这个对象里面体现
	 */
	private static class SQLStatement {

		public enum StatementType {
			DELETE, INSERT, SELECT, UPDATE
		}

		/**
		 * DELETE INSERT SELECT UPDATE
		 * 四大皆空
		 */
		StatementType statementType;
		/**
		 * 更新用的set值
		 */
		List<String> sets = new ArrayList<String>();
		/**
		 * 查询的值
		 */
		List<String> select = new ArrayList<String>();
		/**
		 * 表名
		 */
		List<String> tables = new ArrayList<String>();
		/**
		 * 连接
		 * join
		 * innerJoin
		 * outerJoin
		 * leftOuterJoin
		 * rightOuterJoin
		 */
		List<String> join = new ArrayList<String>();
		List<String> innerJoin = new ArrayList<String>();
		List<String> outerJoin = new ArrayList<String>();
		List<String> leftOuterJoin = new ArrayList<String>();
		List<String> rightOuterJoin = new ArrayList<String>();
		/**
		 * where 过滤条件
		 */
		List<String> where = new ArrayList<String>();
		/**
		 * 分组后过滤条件
		 */
		List<String> having = new ArrayList<String>();
		/**
		 * 分组
		 */
		List<String> groupBy = new ArrayList<String>();
		/**
		 * 排序
		 */
		List<String> orderBy = new ArrayList<String>();
		List<String> lastList = new ArrayList<String>();
		/**
		 * 查询 行数
		 */
		List<String> columns = new ArrayList<String>();
		/**
		 * 插入 values
		 */
		List<String> values = new ArrayList<String>();
		boolean distinct;

		public SQLStatement() {
			// Prevent Synthetic Access
		}

		// 几个影响mybatis的 sql 拼接操作


		/**
		 * 通过引用地址值的传递,然后修改值
		 * 主要传递一个 builder对象,就是一个Appendable 对象
		 * 针对Appendable 拼接一些操作 根据数据库中的keyword 以及相关的参数组
		 * 主要是用来 抽象方法  拼sql
		 *
		 * @param builder     拼接对象
		 * @param keyword     关键字串          MySQL关键字
		 * @param parts       一系列参数          一些列集合   select参数  where条件 group by   就是参数
		 * @param open        左开               左边开头
		 * @param close       右闭              右边开头
		 * @param conjunction 连词        连词
		 */
		private void sqlClause(SafeAppendable builder, String keyword, List<String> parts, String open, String close,
		                       String conjunction) {
			if (!parts.isEmpty()) {
				if (!builder.isEmpty()) {
	 	 			// 不空就换行
					builder.append("\n");
				}
				// 添加关键字
				builder.append(keyword);
				// 空格隔开 要不解析错误
				builder.append(" ");
				// 添加一个左操作
				builder.append(open);
				String last = "________";
				for (int i = 0, n = parts.size(); i < n; i++) {
					String part = parts.get(i);
					// 有参数,并且参数不是 and 和 or   就添加连接
					if (i > 0 && !part.equals(AND) && !part.equals(OR) && !last.equals(AND) && !last.equals(OR)) {
						// 其实就是通过 conjunction 来连接 part
						builder.append(conjunction);
					}
					// 例如 select 参数需要用,    where 参数需要用 and
					builder.append(part);
					last = part;
				}
				// 添加右包
				builder.append(close);
			}
		}

		/**
		 * select 拼接
		 *
		 * @param builder
		 * @return
		 */
		private String selectSQL(SafeAppendable builder) {
			// select distinct a,b,c
			if (distinct) {
				sqlClause(builder, "SELECT DISTINCT", select, "", "", ", ");
			} else {
				sqlClause(builder, "SELECT", select, "", "", ", ");
			}
			// select distinct a,b,c FROM table1,table2
			sqlClause(builder, "FROM", tables, "", "", ", ");
			// select distinct a,b,c FROM table1,table2 JOIN table3 JOIN table4
			joins(builder);
			// select distinct a,b,c FROM table1,table2 JOIN table3 JOIN table4 WHERE (a AND b)
			sqlClause(builder, "WHERE", where, "(", ")", " AND ");
			// select distinct a,b,c FROM table1,table2 JOIN table3 JOIN table4 WHERE (a AND b) GROUP BY a,b
			sqlClause(builder, "GROUP BY", groupBy, "", "", ", ");
			// select distinct a,b,c FROM table1,table2 JOIN table3 JOIN table4 WHERE (a AND b) GROUP BY a,b HAVING (a AND b)
			sqlClause(builder, "HAVING", having, "(", ")", " AND ");
			// select distinct a,b,c FROM table1,table2 JOIN table3 JOIN table4 WHERE (a AND b) GROUP BY a,b HAVING (a AND b) ORDER BY a,b
			sqlClause(builder, "ORDER BY", orderBy, "", "", ", ");
			return builder.toString();
		}

		private void joins(SafeAppendable builder) {
			sqlClause(builder, "JOIN", join, "", "", "\nJOIN ");
			sqlClause(builder, "INNER JOIN", innerJoin, "", "", "\nINNER JOIN ");
			sqlClause(builder, "OUTER JOIN", outerJoin, "", "", "\nOUTER JOIN ");
			sqlClause(builder, "LEFT OUTER JOIN", leftOuterJoin, "", "", "\nLEFT OUTER JOIN ");
			sqlClause(builder, "RIGHT OUTER JOIN", rightOuterJoin, "", "", "\nRIGHT OUTER JOIN ");
		}

		private String insertSQL(SafeAppendable builder) {
			// INSERT INTO table
			sqlClause(builder, "INSERT INTO", tables, "", "", "");
			// INSERT INTO table(a,b,c)
			sqlClause(builder, "", columns, "(", ")", ", ");
			// INSERT INTO table(a,b,c)VALUES(a,b,c)
			sqlClause(builder, "VALUES", values, "(", ")", ", ");
			return builder.toString();
		}

		private String deleteSQL(SafeAppendable builder) {
			// DELETE FROM table
			sqlClause(builder, "DELETE FROM", tables, "", "", "");
			// DELETE FROM table WHERE (a AND b)
			sqlClause(builder, "WHERE", where, "(", ")", " AND ");
			return builder.toString();
		}

		private String updateSQL(SafeAppendable builder) {
			// UPDATE table
			sqlClause(builder, "UPDATE", tables, "", "", "");
			// UPDATE table JOIN a
			joins(builder);
			// UPDATE table JOIN a SET a,b,c
			sqlClause(builder, "SET", sets, "", "", ", ");
			// UPDATE table JOIN a SET a,b,c WHERE (a AND b)
			sqlClause(builder, "WHERE", where, "(", ")", " AND ");
			return builder.toString();
		}

		public String sql(Appendable a) {
			SafeAppendable builder = new SafeAppendable(a);
			if (statementType == null) {
				return null;
			}

			String answer;

			switch (statementType) {
				case DELETE:
					answer = deleteSQL(builder);
					break;

				case INSERT:
					answer = insertSQL(builder);
					break;

				case SELECT:
					answer = selectSQL(builder);
					break;

				case UPDATE:
					answer = updateSQL(builder);
					break;

				default:
					answer = null;
			}

			return answer;
		}
	}

	// 理解以下引用的地址的值 传递

	public static void main(String[] args) {
		StringBuilder sb = new StringBuilder("sb");
		add(sb);
		System.out.println(sb.toString());
		Person person = new Person();
		set(person);
		System.out.println(person.getName());
	}

	static class Person {
		private String name;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	// 引用地址 值的传递

	private static void add(StringBuilder sb) {
		sb.append("加加加");
	}

	private static void set(Person person) {
		// 其实是一个引用,所以要copy呢
		Person person2 = person;
		person2.setName("开心");
	}
}
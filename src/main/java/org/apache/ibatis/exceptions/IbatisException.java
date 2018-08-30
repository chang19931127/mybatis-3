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
package org.apache.ibatis.exceptions;

/**
 * @author Clinton Begin
 * 自定义异常,这个就是mybatis中的异常
 * 主要这里是运行是异常,什么时候使用运行时异常,程序没有办法解决的错误
 * 如果针对可以解决的异常就可以使用check 异常,因此业务异常基本都是uncheck 异常
 */
@Deprecated
public class IbatisException extends RuntimeException {

	private static final long serialVersionUID = 3880206998166270511L;

	public IbatisException() {
		super();
	}

	public IbatisException(String message) {
		super(message);
	}

	public IbatisException(String message, Throwable cause) {
		super(message, cause);
	}

	public IbatisException(Throwable cause) {
		super(cause);
	}

}

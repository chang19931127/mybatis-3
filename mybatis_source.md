# mybatis 源码阅读

一个不错的开源框架

1. 健全的文档，源码库下 /site/xdox 提供了全面的文档
2. 代码设计较好，运用了很多的模式

     MyBatis 是一款优秀的持久层框架，它支持定制化 SQL、存储过程以及高级映射。MyBatis 避免了几乎所有的 JDBC 代码和手动设置参数以及获取结果集。MyBatis 可以使用简单的 XML 或注解来配置和映射原生信息，将接口和 Java 的 POJOs(Plain Old Java Objects,普通的 Java对象)映射成数据库中的记录。

> 因此我们需要来阅读源码，打通java和数据库的连接，学习代码编写风格

[Mybatis快速入门](http://www.mybatis.org/mybatis-3/configuration.html)

三个重要对象

- SqlSessionFactoryBuilder(创建即可废弃)
- SqlSessionFactory(单例存在)
- SqlSession(线程存在)

>一个可配置的流程：properties->xml->应用程序

## 目录结构

### src/main/java 源代码位置

    annotation      注解包，一种替代xml的方式
    binding         绑定包，用于将接口和语句进行绑定
    builder         建造包，用于通过接口来创建语句
    cache           缓存包，用来进行缓存操作
    cursor          游标包，用来进行游标操作
    datasource      数据源包，用来管理数据源的操作
    exceptions      异常包，用来放自定义异常
    executor        执行包，用来管理语句执行的操作
    io              资源包，对资源管理获取的操作
    jdbc            数据库包，对jdbc操作管理
    lang            lang包，就两个注解，标识什么版本的javaAPI
    logging         日志包，记录进行的操作。
    mapping         映射包，java类和概念的映射
    parsing         解析包，解析某些内容
    plugin          插件包，一些插件的操作
    reflection      反射包，一些关于反射操作的整理
    scripting       脚本包，一些语言的操作
    session         回话包,关于回话的管理操作
    transaction     事务包，和事务相关的操作
    type            类型包，数据主要就是类型转化的操作

### src/test/java 测试代码位置

我们可以借助测试代码来进行源码的debug

## mybatis主要功能

### 多配置环境

MyBatis 可以配置成适应多种环境，这种机制有助于将 SQL 映射应用于多种数据库之中， 现实情况下有多种理由需要这么做。例如，开发、测试和生产环境需要有不同的配置；或者共享相同 Schema 的多个生产数据库， 想使用相同的 SQL 映射。许多类似的用例。

尽管可以配置多个环境，每个 SqlSessionFactory 实例只能选择其一

每个数据库对应一个 SqlSessionFactory 实例

### 配置内容读取

1. properties

    首先properties文件读取，其次是配置xml文件中读取，最后代码中参数读取

    优先级，方法参数作为的属性，配置xml文件中的属性，properties文件中的属性
2. xml

    读取配置文件和映射文件

### 类型转化

typeHanler：无论是 MyBatis 在预处理语句（PreparedStatement）中设置一个参数时，还是从结果集中取出一个值时， 都会用类型处理器将获取的值以合适的方式转换成 Java 类型。下表描述了一些默认的类型处理器。

mybatis从数据库映射出来的java对象通过objectFactory来进行创建，引入到java中

### 插件

MyBatis 允许你在已映射语句执行过程中的某一点进行拦截调用。默认情况下，MyBatis 允许使用插件来拦截的方法调用包括：

- Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)
- ParameterHandler (getParameterObject, setParameters)
- ResultSetHandler (handleResultSets, handleOutputParameters)
- StatementHandler (prepare, parameterize, batch, update, query)

可以在插件中埋点，日志，记录，等等，想要的功能

## 个人思考

    Mybatis就如同是通过xml来写java关于数据库的操作，解决java程序员拼sql出错的痛点。帮助程序员在sql执行的时候动态观察，异常提前抛出。RuntimeException

## 关键类解析

一个和使用的类做详细介绍,功能类就阐述功能

我们阅读源码的方式，通过从包入手，尽可能的阅读工具包，然后大致了解功能之后，再去通过工程主线，启动，加载，执行，等流程去阅读代码

### Resources(资源加载)

org.apache.ibatis.io

资源包下主要是围绕Resources这个类进行操作，通过这个类来进行资源的加载，通过类加载器，然后得到URL，File，InputStream，Reader，Class对象等等

ResolverUtil这个工具类不能丢掉,他就是用来实现扫包的,一个是获得某个类的子类，一个是获得某个有注解的类，需要**扫包代码**可以这里找

VFS这个类最强大，就是这个类来通过类加载器获得url中的资源,加载某个path下的所有资源文件(包含子目录)

- 单例模式,通过内部静态类懒加载来实现
- 门面模式把,封装了很多类加载器,但你只需要操作ClassLoaderWrapper
- 资源加载就去找类加载器，然后去学一波类加载器把以及URL，File等java封装的对象

### MetaClass and MetaObject (关于对象的操作)

org.apache.ibatis.reflection

反射包下主要是围绕这两个类,通过构造这两个类,一个是关于Class相关的反射操作，一个是关于对象相关的反射操作,可以根据反射调用方法,获得属性,获得内容,等等，这里面有一个工厂模式.都是通过工厂来创建相对应的操作部分,所以如果对反射很感兴趣或者有所相关的操作可以参考这个包中的相关实现.

- 工厂模式,特殊功能都有工厂来创建
- 包装模式,封装起来，提供更加方便的方法
- 私有构造器,对外提供方法来调用构造
- 良好的面向对象设计,各个类分工明确,被主要类引用
- XXXUtils 工具类都对jdk进行了封装，要学会这种封装思想

虽说反射性能较低,但是ReflectorFactory这个类就很巧妙的将反射后的对象缓存起来,这样子反射对我们来说还会造成性能影响么？

### 插件包

org.apache.ibatis.plugin

mybatis的拦截器调用把，核心接口Interceptor,核心注解Intercepts,Signature 这三个一起使用构成拦截器,InterceptorChain来不断的代理多个Interceptor,针对每一个Interceptor会通过Plugin获得一组Signature(type 和method,args对应的才有,如果没有就报错了),然后最终代理对象，将对象封装到Invocation这个类中

#### 异常包

org.apache.ibatis.exceptions

最可能的自己实现自己的异常，搭配合适的异常名称，但是统一一个异常入口，然后其他自定义的继承这个统一的异常，反正不要使用RuntimeException或者Exception，继承异常的代码就很好写了

#### 解析包

org.apache.ibatis.parsing

负责表达式的解析把GenericTokenParse通用的前后缀,通过TokenHandler进行处理，可以使用的PropertyParse来进行${}和properties的替换，以及XPathParse和XNode关于XPath的处理针对Node节点做封装，然后通过Xpath方便做处理，主要还是对字符串进行解析处理替换的

学到一点就是Properties这个jdk提供的工具很好用的。在java里面去获取值，或者存储一个key-value类型的值，其实键值对应用的还是挺多的

#### 游标包

org.apache.ibatis.cursor

游标,在有大量查询的情况下,通过游标的特性,以及迭代器的实现,来进行移位去查询数据,以游标的方式去拉取数据.主要就是一个Cursor接口,这个接口实现了Closeable和Iterator接口,这下你就可以明白了把.主要是这个方法fetchNextObjectFromDatabase()在游标打开的情况下，可以去不断的拉去数据,主要还是围绕结果集

#### 缓存包

org.apache.ibatis.cache

这个包需要好好了解下,因为缓存的思想很重要！。Cache缓存抽象接口提供简单的api get put remove 然后提供一个CacheKey封装了ArrayList来提供复杂的key 然后通过TransactionalCacheManager进行缓存的管理。缓存是什么,可以帮助我们缓存部分数据,那么如何寻找我们的缓存数据,就需要一个标识key,这个java中的hashMap就刚好满足。不过缓存要考虑很多的内容,缓存过去,缓存穿透等等

用到了装饰模式，提供各种功能的缓存，介绍mybatis提供的缓存,外部封装一个简单的缓存然后通过方法,提供更强的方法

代理和装饰模式的区别就是装饰模式关注增加方法增加新功能,而代理模式专注控制方法

- PerpetualCache最简单的缓存,封装HashMap(也可也考虑通过ConcurrentHashMap)仅提供get put remove 方法，其他所有操作用户自己去进行控制，无限put 内存会不会有危险哈哈哈
- BlockingCache阻塞缓存,如果访问的时候没有命中缓存会设置一个锁,知道数据被缓存,以此来阻止命中数据库,通过ReentrantLock来锁缓存的key,如果获取锁失败通过java中的异常来使程序终结,也不会导致命中数据库,整体来讲就是,锁key以key维度来管理缓存的操作

- FifoCache先进先出缓存,内部封装一个Deque双端队列以及一个size容量,如果容量满了就要remove key通过FiFo原则将First的进行remove来控制缓存的容量

- LruCache最近最少使用,内部封装一个重写removeEldestEntry方法的LinkedHashMap里面存储key,key然后就是LinkedHashMap这个Map的特性了,好好去了解下他的get方法和put方法真的很有收获,自带Lru特性(**jdk源码**是不是需要多次阅读啊,这里稍微多嘴,构造函数可以决定是是否使用lru进行排序,然后通过removeEldestEntry方法的返回值来决定是否remove掉Eldest那个entry)

- LoggingCache给缓存提供日志功能,内部通过封装一个Log对象,然后在getObject方法的时候计算一个缓存命中率名且打印出来,其实就是提供一个日志功能而已

- SerializedCache可以序列化的缓存,内部直接就将我们需要缓存的value进行序列化操作,通过内存流和对象流进行操作转化前提条件就是value缓存的对象需要可序列化

- ScheduledCache有计划的缓存,这里通过内部封装一个lastClear时间和时间间隔,然后只要针对缓存有操作就回去判断当前时间和上次清楚时间的差如果大于时间间隔就执行Cache.clean进行全部缓存清理,可以理解成触发间隔清理缓存把

- SynchronizedCache通过不换,直接Synchronized关键字利用虚拟机来达到缓存的效果进行缓存,调用缓存相关的方法的时候这些个方法都是synchronized修饰的

- SoftCache软引用缓存,内部封装一个Deque用来强引用,一个ReferenceQueue来形成弱引用队列,以及一个强引用个数,然后进行缓存操作,get缓存就放到强引用队列中,其他不再强引用队列中就会被垃圾回收器进行回收。强引用队列通过移除最早进入的策略来进行.主要是ReferenceQueue.poll方法的巧妙

- WeakCache和SoftCache比较类似这两种缓存,我都没有模拟出来让引用队列有值,可能对**引用**还是不够了解,下去之后还是需要好好了解以下java中的引用和引用队列啊,最好用代码来验证下软引用不需要gc就可以清除,弱引用需要gc才会清除,强引用gc也不会清除,除了强引用其他引用都和ReferenceQueue有关系

- TransactionalCache事务缓存,内部通过封装一个Map用来存放所有put操作一个Set用来存放get为命中操作,然后commit的时候将所有Map和Map中没有的key都put到真正的缓存对象中,如果rollback,将未命中的都从缓存对象中remove,也就是通过另外一个容器来进行一步临时操作

    总结,针对缓存操作应该也都是针对key进行操作为了组织程序穿透缓存,所以关于缓存的实现只要针对key多考虑考虑就没有问题毕竟通过key来映射缓存的内容啊
    在回忆引用,强引用，弱引用(需要gc去清除)，软引用(仅只剩下软引用时自动清楚)，虚引用(gc后留下一个标记)，后三个都需要ReferenceQueue的poll方法可以快速的清楚,当然也可以通过jvm去清楚

#### 日志包

org.apache.ibatis.logging

mybatis中统一Log门面然后支持很多风格的日志。日志实现较多,通过门面进行统一,然后在application中进行使用。通过内部定义接口Log和LogFactory,LogFactory这个类很重要通过工厂模式然后内部封装一个Constructor对象,然后static代码块来给这个对象赋值,之后通过getLog方法直接反射获得实例,这个实例就是针对开源日志进行封装(slf4j,commonlog,log4j,sout,null)。最重要的就是针对各个开源日志的api进行封装,所以要了解各个开源日志的api,按照类中写的顺序一次加载,知道Constructor对象!=null的时候就不run()线程了,也可以通过配置直接调用useCustomLogging()方法,来实现用户自定义日志使用(<settingname="logImpl" value="LOG4J"/>)

主要使用的设计模式就是工厂模式和适配器模式封装开源日志库适配成Log日志接口

#### SQL操作

org.apache.ibatis.jdbc

这个包就厉害了,真正的面向对象思想,Null这个类将数据库和java类型进行映射,我们操作数据库都是使用的SQL语句,AbstractSQL将我们对数据库的大部分SQL语句都通过这个类或者子类来拼成串,最终在通过ScriptRunner这个类通过连接数据库建立网络连接,然后将我们拼接的SQL发送到数据库并获得结果。特别棒。要明白jdbc已经将我们使用数据库变得简单,我们只用发送SQL就可以了,于是乎mybatis就将SQL进行拆解然后通过面向对象的形式让我们用户去使用。多看代码钻研把。加油

在总结下,这个包涉及到一些JDBC的API记得了解一下JDBC有一个默认的数据库和java类型的转化,但是Mybatis又通过TypeHandler封装自定义了一层,这样子很灵活！赞！

几个sql语句拼接总结,也对SQL有一个学习

update 和 select 都可以配合连接查询 JOIN

```java
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
```

通过传入不同类型的变量然后拼接出不同的sql语句,是不是很棒,重点拼接后的SQL格式还比较美观.

然后归根到ScriptRunner这个类来进行SQL脚本语句连接数据库执行的操作。简单的打开数据库连接,针对是否自动事务提交做兼容处理,这里直接使用Statement语句执行,主要针对MySQL规则注释打印出来,语句打印出来，以及结果打印出来,还是值得了解的，就是一个txt.sql文件被java代码执行的过程。多了解程序是干什么的,这样子能实现什么，有什么价值,这个程序就可以理解执行txt.sql的对吧，其实解析还是挺重要的东西，也可以理解成翻译送达把

以及SqlRunner真正是执行SQL语句的里面可以选择是否使用生成key以及内部通过TypeHandler封装了PreparedStatement.setParemeter()方法使用java中的对象和MySQL中的对象映射的更加灵活,通过结果集的元数据对象获得culomns的jdbc过来的类然后在通过Mybatis的TypeHandler进行进一步跟进，这个类还包含了数据库的增删改差,特别风骚

    普及一个知识

    1. DML(data manipulation language)：

    它们是SELECT、UPDATE、INSERT、DELETE，就象它的名字一样，这4条命令是用来对数据库里的数据进行操作的语言

    2. DDL(data definition language)：

    DDL比DML要多，主要的命令有CREATE、ALTER、DROP等，DDL主要是用在定义或改变表(TABLE)的结构，数据类型，表之间的链接和约束等初始化工作上，他们大多在建立表时使用

    3. DCL(Data Control Language)：

    是数据库控制功能。是用来设置或更改数据库用户或角色权限的语句，包括(grant,deny,revoke等)语句。在默认状态下，只有sysadmin,dbcreator,db_owner或db_securityadmin等人员才有权力执行DCL

这个包就是拼SQL执行SQL通过封装JDBC的API基本也就是Mybatis的核心操作了。一定要问为什么？

之前Mybatis通过这个包下的SelectBuilder和SqlBuilder通过构建者模式来进行SQL的拼接但是现在这种方式更好

最后一句还是好好阅读下AbstractSQL这个类里面都是拼接以及这种模式

#### 类型包

org.apache.ibatis.type

上面我们认识到MySQL通过JDBC数据类型转化到java中,但是这个类型设计的不够灵活怎么办,Mybatis就通过这个包来进行java对象的转化了TypeHandler接口和TypeReference抽象类。通过BaseTypeHandler这个类将TypeReference适配到TypeHandler这个接口上！其他的类都是在围绕BaseTypeHandler操作了，使得TypeReference一个类型包装类拥有了TypeHandler类型转化的能力。这里就需要table了

来一段针对泛型类获得泛型的操作,就是一个类可以通过java反射的API来获取类上面的注解和类上面的泛型参数,然后哈哈，你就可以做你想做的事情了,这样就意味着你通过注解或者泛型参数将类的功能增强了哈哈，fastJson中的TypeReference好像也是这么设计的。！！！！！！！这样子直接可以得到一个类的泛型参数！！！邦邦达，是不是又要装逼学习一波API        反射包反射API针对框架开发很有用的

逻辑封装完毕后就是简单的通过PreparedStatemetn的APIsetXxx()getXxxx 进行不同的TypeHandler 中去细分操作,这样的作用是啥呢?是帮助我们用户去写getXxx setXxx么?暂时这么认为把，就是说mybatis帮助我们去进行类型转化。主要还是java类型转化成数据库类型以及通过数据库类型转化成java类型

这个报下面主要就是一大堆TypeHandler 然后被TypeHandlerRegister引用并且投入使用并且配合Alias和MappedTypes等注解来实现类型适配，并且可以指定包进行别名，别名的目的就是别名Class 最终还是注册到TypeHandlerRegister，这个对象就拥有了所有的mybatis中类型转化的仓库了并被其他地方获得TypeHandler 和 添加------这个类主要就是一个大工厂,将系统已知的或者通过方法注册的或者扫包用户自定义的统统存储起来,然后提供验重和得到对应的TypeHandler进行 参数的set和get,可以充当TypeHandler大容器被他人使用

还有两个注解MapperedJdbcTypes,主要用于java对象和多个JdbcType进行映射,MappedTypes这个注解主要修饰TypeHandler 用于将Jdbc来映射java对象了

这么理解java对象我们可控JdbcType我们不可控,所以我们需要TypeHandler来让其可控,哈哈哈

```java
public abstract class TypeReference<T> {

    private final Type rawType;

    protected TypeReference() {
        rawType = getSuperclassTypeParameter(getClass());
    }

    Type getSuperclassTypeParameter(Class<?> clazz) {
        // 得到泛型类型Type
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass instanceof Class) {
        // try to climb up the hierarchy until meet         something useful
        if (TypeReference.class !=      genericSuperclass) {
            return getSuperclassTypeParameter(clazz.getSuperclass());
            }

            throw new TypeException("'" + getClass() + "' extends TypeReference but misses the type parameter. "
                    + "Remove the extension or add a type parameter to it.");
        }

        Type rawType = ((ParameterizedType)         genericSuperclass).getActualTypeArguments()[0];
        // TODO remove this when Reflector is fixed to      return Types
        if (rawType instanceof ParameterizedType) {
            rawType = ((ParameterizedType) rawType)     .getRawType();
        }

        return rawType;
    }

    public final Type getRawType() {
        return rawType;
    }
}
```

#### 事务包

org.apache.ibatis.transaction

身为一个Dao层框架,肯定会帮助我们来管理数据库的事务操作，一般的数据库也都支持事务操作，因为事务的存在我们的数据才能准确，那么mybatis通过transaction包下的操作来帮助我们来封装事务API。Transaction接口必须要有(事务肯定和Connection有关系).外加一个TransactionFactory接口来管理事物接口啊(通过Connection,或者DataSource来获取事务)，设计事情要定要会分析。很简单的工厂模式第一对实现(JdbcTransaction,JdbcTransactionFactory),主要还是Transaction的封装实现操作把,整体没啥可看的就是封装Connection然后针对Connection做一个关于事务相关的操作ManagedTransaction可以通过Properties来进行配置操作,这个包下暂时没有什么特色

#### 数据源包

org.apache.ibatis.datasource

一般情况下我们不会直接使用Connection,毕竟使用连接池可以防止Connection的不断开启关闭,提高性能,但是DataSource(java的API)有很多实现因此工厂模式又来了DataSourceFactory,有的时候希望对象可以通过配置来改变程序操作的时候可以通过setProperties来传入Properties对象来改变

可以通过外置配置文件加载到java中Properties文件然后传递过来获得数据库连接池,嘻嘻嘻嘻，主要的责任就是DriverManager.getConnection,以及通过反射等操作来获取Driver程序,所以这里是不是需要复习下jdbc中关于Driver DriverManager 相关操作啊哈哈,当然DriverManager直接获得的Connection但是DataSource也可以获取Connection,哈哈,直接使用还是通过连接池使用，你们懂得

可以了解下mybatis中PooledDataSource的实现,实现了一个数据库连接池,一个连接池应该可伸缩,应该有一个限度值，并且可以监控,并且可以心跳检测远端服务,通过和数据库发送(NO PING QUERY SET)来确保和数据库连接的正常操作。然后有最大活跃线程,最大空闲线程，以及checkout(观察时间)等待时间。可以理解成复用现有的Connection,来一个请求就追加一个Connection如果到最大的限制就去等待.如果等待时间过长.就移除一个使用最久的Connection然后在新建一个Connection去进行操作。主要就是两个操作popConnection和

```伪代码
pushConnection
activeCon.remove(con)
if(con有效)
    if(空闲<小于最大空闲)
        新建一个Con并加入到空闲
    else
        将这个con设为无效
else
    badcon+
popConnection
if(有空闲con)
    从空闲remove 并得到这个con
else
    if(activeCon < 最大活跃)
        直接newCon
    else
        去除活跃Con中最早的
        if( 等待时间 > 最大等待时间)
            活跃线程中移除最早的con，并创建一个con
        else
            等待..
```

#### 绑定包

org.apache.ibatis.binding

mybatis给我们提供编写接口,然后通过接口的相关操作来实现sql语句的调用,那么接口如何转化成SQL语句呢，并且我们写的接口如何存储呢？MapperRegistry给你存储的地方并且对外提供了相应操作的接口getMapper和addMapper,同样使用了工厂模式和建造者模式重点在内部MapperAnnotationBuilder这个类。

整体来一套逻辑：MapperRegistry 本地缓存了所有的Mapper对象(通过Mapper接口和MapperProxyFactory),然后MapperProxyFactory本地缓存了所有MapperMethod对象(通过method和MapperMethod映射)。然后MapperProxy闪亮登场，使用了jdk动态代理过滤掉Object方法和接口默认方法然后 在构造成MapperMethod对象，这个对象可以说是我们使用mybatis 最终涉及到的类。这个类会将Mapper接口和Mapper映射文件关联起来。然后通过SqlCommand和其操作执行最终的execute方法。这里面就是增删改查以及其他类型的SQL操作。所以binding 是将接口通过映射文件转化对象。然后在交给Mapping包中的类去负责接下来的操作！MapperMethod中的 execute方法好好理解下 其实还是主要调用SqlSession中的方法。

涉及到的模式，就是本地缓存。工厂模式，以及代理模式

#### 构造包

org.apache.ibatis.builder

binding包中的MapperRegistry使用了MapperAnnotationBuilder来构造并解析出，因此我们需要走进buider包中，来看下buider包为mybatis提供了什么支持？初步猜测，xml和注解的解析通过建造者模式，building code ，必然分为xml和注解。

再推进一步，这个包主要的内容估计就是 构造代码 接口的实现代码(通过配置的内容)

抽象父类 BaseBuilder,主要提供了一些功能性方法(被子类所共享),ParameterExpression(Map,里面存储了常用的解析操作)，MapperBuilderAssistant这个类功能很强大,然后针对功能区分出两个小功能类CacheRefResolver和ResultMapResolver。实际还是需要好好阅读MapperBuilerAssistant(继承了BaseBuilder)这个类，这个包主要使用了构造者模式，来构造很多对象，给Mapping包做基础,这些关于Mapping包中的对象，我们到Mapping包中再去看，(ParameterMapping，ResultMapping，MappedStatement，BoundSql)等等相关的

这个包还会遇到两个解析，一个xml的解析，一个注解的解析，这两个使我们用mybatis需要写的东西。表面一大堆 xml,注解 最终还是要转化成类 ，然后去做某些操作的

xml 解析有3种，一种是XMLConfigBuilder 配置的解析(解析完毕封装到Configuration类中)，一种XMLMapperBuilder 映射的解析,一种是XMLStatementBuilder sql语句的解析。凡是请记住XMLConfigBuilder加载并且处理创建XMLMapperBuilder然后叫Mapper去进行处理,Mapper构造XMLStatementBuilder去处理

注解就一个MapperAnnotationBuilder解析

其实Properties 这个东西使用的地方挺多的，虽说Effective Java说这是一个不好的设计,但是在应用场景中Properties这种key,value 很常见啊

所以一定要学会人家软件架构的能力,写代码之前尽可能的出一些设计图,然后在围绕设计图去编写代码,这样子就能攻做到宏观控制

用到的抽象父类，构造者模式

#### 回话包

org.apache.ibatis.session

连接数据库运行的时候就会产生会话,这个包也就可以认为是针对会话的管理相关吧

通过配置 什么事物 怎么执行 去执行

一个会话链

- SqlSesionFactoryBuilder(类): 构造SqlSessionFactory 需要读文件因此比较重
- SqlSessionFactory: 构造  SqlSession
- SqlSession: 执行相应的会话操作 ResultHandler
- ResultHandler: 根据ResultContext 来执行操作
- ResultContext: 默认实现都在executors包中

一个辅助工具抽象

- Configuration: 超级大国,其他都是他的子民,这个类里面基本贯穿了Mybatis的所用东西。。。可以根据这个类为出发点去做配置操作,剩下的就是 executor了
- RowBounds: 数据库 分页的 抽象类 limit offset

一个特殊类

- SqlSessionManager: SqlSession管理类 实现了implements SqlSessionFactory, SqlSession 估计可以更好地去维护 我们的SqlSessionFacotry和SqlSession 两个常用的会话对象       使用了装饰模式,或者门面统一使用这个类就好吧  连接这种资源 ThreadLocal 不能忘啊 这个类**值得回味**  内部通过代理对象 然后起到SqlSession对象的复用程度

#### 执行包

org.apache.ibatis.executor

真正触发和jdbc进行交互的都在这个包下面，让我们深入这个包来了解里面的内容吧

首先思考 executor 都需要做什么

1. key生成
2. 代理来加载result
3. 参数处理 (可插件)
4. 结果处理
5. 结果集逻辑处理 (可插件)
6. 语句处理 (可插件)
7. 正儿八经的executor (可插件)

分开理解吧,KeyGenerator 关于主键的操作(jdbc默认的相关操作都是行,但是我们需要返回id mybatis帮助我们解决了 其实就是帮助我们调用Statement.getGenerateKeys方法然后填充下 以及 xml:selectKey 中对应的key产生语句)

这里面用了javassist和cglib 两种代理方式可以了解一下

大致内容分析完毕，那么基本就是接口定义和实现了。

#### 映射包

org.apache.ibatis.mapping

这要是这个包将 来辅助对象和sql进行映射操作，我们去一探究竟把

个人猜测，这个包就是一个辅助包，主要就是封装一层关系，映射关系，将jdbc和java对象映射关系进行封装，这样的好处就是可以缓存起来，来回的数据转化效率很高

SqlSource接口->配合参数获取BoundSql
BoundSql类里面有ParameterMapping 这个类就是封装参数和jdbc映射的关系对象。

ParameterMapping 这个类很有意思,通过自己内部的静态类来 构造自己,并且自己可以修复数据并校验

主要是两个接口

里面疯狂使用建造者模式啊,建造者模式可以帮助我们方便的给一个对象增加属性，并且可以校验，来保证我们的对象是合法的

**MappedStatement** Mybatis 中该包下面最主要类，直接组合起来所有的操作。只要从这个类看起就好，如果复习的化

CacheBuilder 来学习一波建造者模式，都是通过静态内部类来构造外部的对象,是为了什么要这么做啊

#### 语言包

org.apache.ibatis.scripting

可以得知这两个类十分重要 ParameterHandler和SqlSource，一个是映射包中的，一个是执行包中的，语言包主要就是解析后的东西再进一步加工处理。
SqlSource可以通过mapper xml 或者 注解 配置后然后进一步加工获得，都需要第一步Configuration(builder包中的)，按功能分包

这个包主要就是通过操作将Xml 语句 转化成java 然后在通过java 进行操作最终拼成Sql

我们通过Xml中的sql就在这个包里面可以找到XMLScriptBuilder这个类中有trim,where,set,foreach,if,choose,when,otherwise,bind.等

这里主要就是通过解析XML 然后最终生成Sql语句 这里面的代码关系比较清晰，继承组合，等等，这个源码值得阅读学习,

#### mybatis单侧部分

mybatis提供的单侧挺多的，我们可以进行学习。了解一个好的开源软件该有的单侧，并且再次配合单侧回顾下之前看过的知识点。今天就是搞定数据库和linux相关

## 代码片段

### 有用的代码保留

```java
//类路径下面加载类
Thread.currentThread().getContextClassLoader().loadClass(className);
//类路径下面加载资源
Thread.currentThread().getContextClassLoader().getResources(path);
//通过类加载器加载资源流
Thread.currentThread().getContextClassLoader().getResourceAsStream(path);

//加载properties文件
Properties props = new Properties();
//拿到输入流，上面的通过类加载器拿
props.load(in);


// 一半工程里面都是继承RuntimeException,然后重写一些特殊的构造方法
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

debug 日志如何打印
if (log.isDebugEnabled()) {
    log.debug("Cache Hit Ratio [" + getId() + "]: " + getHitRatio());
}




一个上下文类
public class DefaultResultContext<T> implements ResultContext<T> {

    /**
     * 结果集对象
     */
    private T resultObject;
    /**
     * 结果集个数
     */
    private int resultCount;
    /**
     * 是否停止
     */
    private boolean stopped;

    public DefaultResultContext() {
        resultObject = null;
        resultCount = 0;
        stopped = false;
    }

    @Override
    public T getResultObject() {
        return resultObject;
    }

    @Override
    public int getResultCount() {
        return resultCount;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    public void nextResultObject(T resultObject) {
        resultCount++;
        this.resultObject = resultObject;
    }

    @Override
    public void stop() {
        this.stopped = true;
    }

}




JNDI操作代码
通过Context操作lookup来获取操作
InitialContext initCtx;
Context ctx = (Context) initCtx.lookup(properties.getProperty(INITIAL_CONTEXT));
dataSource = (DataSource) ctx.lookup(properties.getProperty(DATA_SOURCE));



这个类 字节码代理
javassist包
javassist.util.proxy.ProxyFactory
```
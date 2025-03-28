package io.github.aboutou.redisson.lock4j.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @example
 * <p>
 * 一. 正常锁
 * @DistributedLock(lockName = "PreReceiptOrder", key = "#preReceiptOrderChangeBo.entryOrderId")
 * public Boolean change(PreReceiptOrderChangeBo preReceiptOrderChangeBo) {}
 * 二. 联锁
 * 会用到spel
 * 1. 集合选择 【例子】
 *  语法：“(list|map).?[选择表达式]”
 * 	Selection是一种功能强大的表达式语言功能，通过从源集合的条目中进行选择，可以将源集合转换为另一个集合。
 * 	Selection使用的语法为.？[selectionExpression]。它过滤集合并返回包含原始元素子集的新集合。例如，selection可以让我们很容易地获得塞尔维亚发明家的列表，如下示例所示：
 *
 * 	List<Inventor> list = (List<Inventor>) parser.parseExpression(
 *         "Members.?[Nationality == 'Serbian']").getValue(societyContext);
 * 	在list和map上都可以Selection。对于list，将根据每个单独的列表元素评估选择条件。针对map，选择标准针对每个映射条目（Java类型Map.Entry）进行评估。每个map项都有其键和值，可以作为属性访问，以便在选择中使用。
 * 	以下表达式返回一个新map，该映射由原始map的那些元素组成，其中输入值小于27：
 *
 * 	Map newMap = parser.parseExpression("map.?[value<27]").getValue();
 * 	除了返回所有选定的元素之外，您还能检索第一个或最后一个值。要获取与所选内容匹配的第一个条目，语法为。.^ [selectionExpression]。要获取最后一个匹配的选择，语法为.$[SelectionExpression]。
 *
 * 2. 集合投影【例子】
 * 	语法：“SpEL使用“（list|map）.![投影表达式]”
 * 	Projection允许集合驱动子表达式的计算，结果是一个新集合。投影的语法是.![projectionExpression]。例如，假设我们有一个发明家列表，但是想要他们出生的城市列表。实际上，我们想为发明家列表中的每个条目评估“placeofbirth.city”。下面的示例使用投影进行此操作：
 *
 * 	// returns ['Smiljan', 'Idvor' ]
 * 	List placesOfBirth = (List)parser.parseExpression("Members.![placeOfBirth.city]");
 * 	您还可以使用map来驱动投影，在这种情况下，投影表达式针对map中的每个条目（表示为Java Map.Entry）进行评估。跨map投影的结果是一个列表，其中包含对每个map条目的投影表达式的计算。
 *
 * (1) 数组的处理【例子】
 *
 * 	List<Integer> primes = new ArrayList<>();
 *  List<Integer> c = Arrays.asList(2, 3, 5, 7, 11, 13, 17);
 *  primes.addAll(c);
 *  User u = new User();
 *  u.aa = primes;
 *  u.setUserName("aaa");
 * 	Object aa1 = parser.parseExpression(
 *                 "#user.aa.!['aa' + #this + 'ggg']").getValue(context);
 *  Object aa2 = parser.parseExpression(
 *                 "#user.aa.?[#this>0].!['aa' + #this + ',']").getValue(context);
 *
 * (2) 数组对象的处理
 *
 *	public class Book {
 *
 * 		public String name;         //书名
 * 		public String author;       //作者
 * 		public String publisher;    //出版社
 * 		public double price;        //售价
 * 		public boolean favorite;    //是否喜欢
 * 	}
 * 	public class BookList {
 *
 * 		@Autowired
 * 		protected ArrayList<Book> list = new ArrayList<Book>() ;
 *
 * 		protected int num = 0;
 * }
 *
 * 	//将BookList的实例映射为bean：readList，在另一个bean中注入时，进行投影
 *
 * 	//从readList的list下筛选出favorite为true的子集合，再将他们的name字段投为新的list
 * 	\\@Value("#{list.?[favorite eq true].![name]}")
 * 	private ArrayList<String> favoriteBookName;
 *
 * </p>
 * @author tiny
 */
@Documented
@Target({ElementType.METHOD})
@Repeatable(DistributedLocks.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

	/**
	 * 锁的名称。 如果lockName可以确定，直接设置该属性。
	 */
	String lockName() default "";

	/**
	 * lockName后缀
	 */
	String lockKeyPre() default "";

	/**
	 * lockName后缀
	 */
	String lockKeySuffix() default "lock";

	/**
	 * 获得锁名时拼接前后缀用到的分隔符
	 *
	 * @return
	 */
	String separator() default ":";

	/**
	 * 支持spel表达式
	 * <pre>
	 *  名字			位置			描述					示例
	 *	methodName	root 		object				当前被调用的方法名													#root.methodName
	 *	method		root 		object				当前被调用的方法													#root.method .name
	 *	target		root 		object				当前被调用的目标对象												#root.target
	 *	targetClass	root 		object				当前被调用的目标对象类												#root.targetClass
	 *	args		root 		object				当前被调用的方法的参数列表											#root.args[0]
	 *	caches		root 		object				当前方法调用使用的缓存列表											#root.caches[0].name
	 *	argument 	name		evaluation context	方法参数的名字，可以直接#参数名，也可以使用#p0或#a0的形式，0代表参数的索引	#iban、#a0、#p0
	 *	result		evaluation 	context				方法执行后的返回值													#result
	 *
	 * </pre>
	 */
	String key();

	/**
	 * 是否使用公平锁。 公平锁即先来先得。
	 */
	boolean fairLock() default false;

	/**
	 * 是否使用尝试锁。
	 */
	boolean tryLock() default true;

	/**
	 * 锁最长等待时间。 该字段只有当tryLock()返回true才有效。
	 */
	long waitTime() default 3L;

	/**
	 * <p>
	 * 锁超时时间。 超时时间过后，锁自动释放。
	 * 当该值小于等于0时，启用看门狗，自动续锁的超时时间，tryLock=false与true都生效
	 * 建议： 尽量缩简需要加锁的逻辑。
	 * </p>
	 */
	long leaseTime() default 30L;

	/**
	 * 时间单位。默认为秒。
	 */
	TimeUnit timeUnit() default TimeUnit.SECONDS;

}

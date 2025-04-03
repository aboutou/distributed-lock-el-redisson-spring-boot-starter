## 项目简介

- ‌**项目名称**‌：支持el表达式多个key的分布式锁
- ‌**主要功能**‌：提供基于 Redisson 的分布式锁功能的 Spring Boot Starter，用于简化在 Spring Boot 应用中集成和使用分布式锁的过程。

## 主要特性

1. ‌**集成方便**‌：

    - 作为 Spring Boot Starter，项目提供了自动配置的功能，使得在 Spring Boot 应用中集成分布式锁变得更加简单和便捷。
2. ‌**基于 Redisson**‌：
    
    - 利用 Redisson 客户端实现分布式锁功能，Redisson 是一个在 Redis 的基础上实现的 Java 驻内存数据网格（In-Memory Data Grid）。它提供了很多分布式的数据结构和服务，包括分布式锁、分布式集合、分布式地图等。
3. ‌**分布式锁**‌：
    
    - 通过提供分布式锁的实现，可以帮助开发者在多实例部署的 Spring Boot 应用中，解决数据一致性和并发控制的问题。
4. ‌**配置灵活**‌：
    
    - 允许开发者通过配置文件或编程方式灵活配置分布式锁的相关参数，如锁的过期时间、重试次数等。
-

## 如何使用

1. **启用注解**

```
@EnableRedissonLock
```
2. **在类上加入注解**

```
@DistributedLock(key="aa", )
```

3. key支持el表达式

```
/**  
 * 支持spel表达式  
 * <pre>  
 *  名字        位置       描述             示例  
 *  methodName root      object          当前被调用的方法名                                      #root.methodName  
 *  method    root      object          当前被调用的方法                                       #root.method .name  
 *  target    root      object          当前被调用的目标对象                                  #root.target  
 *  targetClass    root      object          当前被调用的目标对象类                                     #root.targetClass  
 *  args      root      object          当前被调用的方法的参数列表                                #root.args[0]  
 *  caches    root      object          当前方法调用使用的缓存列表                                #root.caches[0].name  
 *  argument   name      evaluation context 方法参数的名字，可以直接#参数名，也可以使用#p0或#a0的形式，0代表参数的索引  #iban、#a0、#p0  
 *  result    evaluation     context             方法执行后的返回值                                      #result  
 * * </pre>  
 */
```
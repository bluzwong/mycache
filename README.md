# 为Retrofit添加两级缓存

```java
if (!reader.useRetrofit() && !reader.useRxjava()) { return; }
```
以下内容需熟悉[Retrofit](https://github.com/square/retrofit)([介绍](http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/1109/3662.html)) & [Rxjava](https://github.com/ReactiveX/RxJava)([介绍](http://gank.io/post/560e15be2dca930e00da1083))的使用。

---

Retrofit使用的[okhttp](https://github.com/square/okhttp)([介绍](http://blog.csdn.net/lmj623565791/article/details/47911083))已经对网络请求进行了缓存，但是如果服务器没有支持缓存该怎么办呢？
以以下接口为例:
```java
public interface Api {
    @GET("/result.php")
    Observable<Result> getResult(@Query("value1") String value1, @Query("value2") String value2);
}

Api api = new Retrofit.Builder()
                .baseUrl(...)
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(Api.class);

```

可能会使用这种方式来操作:
```java
Observable<Result> network = api.getResult(...); // 从网络获取数据
Observable<Result> memory = ...; // 从内存缓存获取数据
Observable<Result> disk = ...; // 从硬盘缓存获取数据

Observable<Result> networkWithSave = network.doOnNext(data -> {
  saveToDisk(data); // 网络获取后进行缓存
  cacheInMemory(data);
});

Observable<Result> diskWithCache = disk.doOnNext(data -> {
  cacheInMemory(data); // 从硬盘缓存获取后 再缓存到内存
});

// 将上面的进行整合使用
Observable<Result> source = Observable
    .concat(memory, diskWithCache, networkWithSave)
    .filter(data -> data != null)
    .first();

```

蛤，由于Rxjava大法好以及lambda表达式，这种方式看起来不错，还挺简洁的。但是，如果有很多很多接口都要做缓存呢？岂不是每个都得经过一番这样的处理？
### NUO! NUO! NUO! DON'T REPEAT YOURSELF!
如果能够这样实现,是不是一颗赛艇？
```java
public interface Api {
    @GET("/result.php")
    @MyCache(timeOut = 5000)
    Observable<Result> getResult(@Query("value1") String value1, @Query("value2") String value2);
}
```
## 闷声发大财:
build.gradle
```groovy
compile ('com.github.bluzwong:mycache-lib:0.1.4@aar') { transitive = true }
```
再换一个CallAdapterFactory
```java
Api api = new Retrofit.Builder()
                .baseUrl(...)
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                //.addCallAdapterFactory(RxJavaCallAdapterFactory.create()) // 使用以下代替
                .addCallAdapterFactory(MyCacheRxCallAdapterFactory.create(MyCacheCore.create(context)))
                .build()
                .create(Api.class);
```
使用被 @MyCache(timeOut = 5000) 注解过的api将会进行缓存

---
### 很惭愧，只做了一些微小的工作。谢谢！

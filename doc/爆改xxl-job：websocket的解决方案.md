爆改xxl-job：websocket版本的改造方案

之前改过一版xxl-job，解决了滥用netty的问题，文章链接：[xxl-job滥用netty导致的问题和解决方案](https://blog.kdyzm.cn/post/72) ，后续基于此又做了一些优化修复了一些bug，但是还留下了一个待优化项：xxl-job客户端和服务端必须在同一个网段内，否则服务端调用客户端接口会调用失败。因为像是定时任务触发、日志查看等实际上都是通过服务端调用客户端restful接口实现的功能，这一直让我觉得如鲠在喉，想找机会使用websocket长连接来替代restful接口调用，这些天有空，用了两三天时间，终于把它改了个七七八八。

## 一、websocket版本改造难点总结

首先我的想法是“小改”，也就是说，核心的逻辑不变，只是更改http调用为一条websockt长连接调用，这样才能保证不出大的错，但是一上手，就发现了就算是“小改”，问题也不少。

### 1、同步改造异步

之前说过，客户端暴露了几个restful接口给服务端调用，现在要将restful接口全删掉，改用一条websocket连接，所有的请求都会从这条连接上发起，响应也会从这条websockt连接上返回。先看看restful接口调用时序图：

<img src="https://blog.kdyzm.cn/blog/public/2025/02/08/3ac89f64-8f28-44dd-b116-73dd44c04bf2.png" alt="image-20250208230840931" style="zoom:50%;" />

从上图中可以看得出来http调用很简单，它完全是一个串行化调用的过程，我问你答，然后将调用结果展示在页面上即可。而websocket只有一条连接，它是全双工双向通信的连接，所以可以同时发送和接收消息，消息的处理是基于事件驱动的，代码调用形式如下所示：

``` java
public class WebSocketServer{
    /**
     * 连接打开时被调用
     */
    @OnOpen
    public void onOpen(Session session){
        
    }
    
    /**
     * 连接关闭时被调用
     */
    @OnClose
    public void onClose(Session session) {
        
    }
    
    /**
     * 发生异常时被调用
     */
    @OnError
    public void onError(Throwable error){
        
    }
    
    /**
     * 收到消息时被调用
     */
    @OnMessage
    public final void receiveMessage(String message){
        
    }
}
```

websocket客户端和服务端的形式都如上代码所示，这就导致了所有的请求都是“异步”的，前端页面请求xxl-job-admin是restful接口，但是xxl-job-admin请求client是通过websocket，websocket接受消息是异步的，就算能回调结果，又怎么传达给xxl-job-admin调用点呢？

我在这里使用了`LinkedBlockingQueue` 队列来传输该消息，我们知道，BlockingQueue的特点就是在接收消息的时候有消息就接受消息，如果没有消息就阻塞等待，可以让xxl-job-admin在发送websocket消息之后立即在BlockingQueue上等待消息回调。完整的时序图交互如下所示

<img src="https://blog.kdyzm.cn/blog/public/2025/02/09/74006f1e-57e1-43fb-9dfc-add2760b86de.png" alt="image-20250209155944961" style="zoom: 60%;" />

核心问题实际上是跨线程通信的问题，BlockingQueue非常适合做这个事情。



### 2、websocket重连

服务端使用了spring-boot-starter-websocket，客户端使用了`Java-WebSocket`组件，websocket重连实际上特指客户端重连服务端，重连可能会发生在服务端挂了，或者业务处理没处理好导致websocket连接被中断等情况。

Java-Websocket组件有重连方法reconnect，但是要求在新线程中调用，这样能够保证完全清理掉旧websocke连接遗留的数据以避免bug的产生。调用的时机选择在监听到websocket连接被关闭的时候调用的onClose方法，有趣的是reconnect方法如果调用失败会继续调用onClose方法，经过多次尝试，发现有无法避免的并发请求reconnect方法的情况，为了彻底解决该问题，这里使用了加锁+双重验证的方式

``` java
@Override
public void onClose(int i, String s, boolean b) {
    log.info("websocket closed connection");
    reconnectSelf(this);
}

private void reconnectSelf(ExecuteWebSocketClient client) {
    if (client.isOpen()) {
        return;
    }
    synchronized (this) {
        if (client.isOpen()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            log.info("websocket reconnecting ...");
            //reconnect方法失败会触发执行onClose方法
            client.reconnect();
        });
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }
}
```








官网介绍
https://kotlinlang.org/docs/select-expression.html#selecting-to-send


First of all, select is biased to the first clause. When several clauses are selectable at the same time, the first one among them gets selected. Here, both channels are constantly producing strings, so a channel, being the first clause in select, wins. However, because we are using unbuffered channel, the a gets suspended from time to time on its send invocation and gives a chance for b to send, too.

首先，select 偏向于第一个子句。当多个子句同时可选时，第一个子句会被选中。这里，两个通道都在不断地生成字符串，因此作为 select 中第一个子句的通道会胜出。但是，由于我们使用的是无缓冲通道，a 在其发送调用时会不时被暂停，这也给了 b 发送的机会。





Kotlin 协程中的 `select` 表达式 **默认带有偏向性**，而不是随机选择：

* **偏向第一分支**
  当多个子句（`onReceive`／`onAwait`／`onSend` 等）同时可选时，`select { … }` 会优先选择 **最先声明** 的那个子句，而不会在所有就绪项中随机挑选。 ([kotlinlang.org](https://kotlinlang.org/docs/select-expression.html?utm_source=chatgpt.com), [blog.csdn.net](https://blog.csdn.net/shulianghan/article/details/128488805?utm_source=chatgpt.com))

* **无偏随机的写法**
  如果你确实需要在多个就绪分支中随机（无偏）选择，可以使用实验性的 `selectUnbiased { … }` 构建器，它会在所有可用子句之间均匀随机地挑选一个。 ([blog.csdn.net](https://blog.csdn.net/shulianghan/article/details/128488805?utm_source=chatgpt.com))

---

**示例：**

```kotlin
val a = Channel<String>()
val b = Channel<String>()

// 启动两个协程，不断向 a 和 b 发送数据
launch { while (true) { a.send("from A"); delay(100) } }
launch { while (true) { b.send("from B"); delay(100) } }

// 偏向版：始终更倾向于 a
select<String> {
  a.onReceive { it }   // 始终是第一个分支
  b.onReceive { it }
}

// 无偏版：在 a、b 就绪时随机挑选
selectUnbiased<String> {
  a.onReceive { it }
  b.onReceive { it }
}
```

* 在第一个例子里，若 `a` 与 `b` 同时有数据可接收，系统会 **优先** 走 `a.onReceive`。
* 在第二个例子里，则会在 `a`、`b` 之间 **随机**（无偏）选取。


偏向性并不会因为两个 `delay` 写得不一样就“消失”——它始终存在于 `select { … }` 的实现里，只不过：

1. **偏向性定义的是同时可选时的行为**

   * 当多个分支“同时”就绪，`select` 会优先选第一个声明的分支。
   * 如果两个分支永远不会同时就绪（例如一个永远比另一个早完成），你根本就不会观察到“同时就绪再偏向第一个”的场景，就看不到偏向性效果。

2. **不同的 `delay` 只是降低了“同时就绪”的概率**

   * 比如你给 `a` 延迟 100ms，给 `b` 延迟 150ms，那么在绝大多数轮次里，只有 `a` 先就绪，`select` 自然接收 `a`，不会“比较”两个分支；
   * 但如果网络抖动、调度延迟等原因让两者恰好“同时”到达，那么还是会偏向第一个分支。

3. **如果你需要真随机**

   * 用 `selectUnbiased { … }`，它会在所有就绪分支中做无偏的随机选择，无论它们是不是同时就绪。

---

**小结**：

* **偏向性** 是 `select` 的固有特性，不会被不等的 `delay` “关掉”。
* 不等的 `delay` 只是让你更少看到多个分支“同步就绪”的情况，所以表面上好像“没有偏向”。
* 要想彻底去偏，就得换成 `selectUnbiased { … }`。







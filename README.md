# Hibari
Between Views and Compose, an Android UI building tool.

## 背景
现在官方所推荐的 Views 开发技术，`XML` + `Bindings` 的开发体验并不好，特别是无法动态变化的特点，使其远不如 `Jetpack Compose` 强大。然而，Google 并不想继续为 Views 提供支持，而我又并不喜欢 Compose，因此，这个库便诞生了。

它的核心目标是，为 Views 引入 Compose 的特性，因此，它的代码大量参考和~复制粘贴~了 Compose。

## 设计
让我们来结合源码理解它的设计。

### 1. Modifier 和 Attribute

在 Hibari 中，两个非常重要的概念是 `Modifier` 和 `Attribute`，一切都由它们构成。举例子：

```Kotlin
@Tunable
fun Column(
    modifier: Modifier = Modifier,
    content: @Tunable ColumnScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(LinearLayout::class.java)
            .orientation(LinearOrientation.VERTICAL),
        content = {
            ColumnScopeInstance.content()
        }
    )
}
```

```Kotlin
fun Modifier.orientation(orientation: LinearOrientation): Modifier {
    return this.thenViewAttribute<LinearLayout, LinearOrientation>(uniqueKey, orientation) {
        when (it) {
            LinearOrientation.VERTICAL -> setOrientation(LinearLayout.VERTICAL)
            LinearOrientation.HORIZONTAL -> setOrientation(LinearLayout.HORIZONTAL)
        }
    }
}
```

可以看到，Modifier 存储了 Attribute 的信息，而 ViewClass 以及其他一切信息都是由 Attribute 表示的。

**为什么要有 Attribute？`uniqueKey` 是什么？**

在运行时，Hibari 维护一棵虚拟的节点树，这就是 Node。Node 上有着 Attribute，每当所依赖的状态改变时，Hibari 发起一次重组（近似概念，实际上名为 `Retune`）。重组完毕后，Hibari 会对比新旧两棵节点树，随后应用更改。Attribute 就是这个对比机制存在的基石。

`uniqueKey` 是一个特殊的属性，对它的引用会在编译时被替换为一个 UUID 字符串。将其作为 Attribute key 传给 Hibari 后，Hibari 运行时将会在对比时通过它定位相同的 Attribute。

### 2. @Tunable 函数 - UI 的基本单元
在 Hibari 中，所有 UI 组件都是通过被 @Tunable 注解的函数来定义的。这等同于 Jetpack Compose 中的 @Composable 函数。每个函数都描述了一部分 UI 的外观和行为。

```Kotlin
@Tunable
fun Greeting(name: String) {
    Text(text = "Hello, $name!")
}
```

## 使用
> [!IMPORTANT]
> 目前，该项目的组件封装仍不稳定，不推荐用于生产环境。

**Material Components 是 Hibari 所推荐的组件库，因此，`Text`、`Button` 等函数都是被封装在 hibari-material 模块中的。如果你不想使用 Material Components，或许你需要自己封装@Tunable函数。**

WIP

## 混淆
Hibari 需要以下规则来确保运行时工作正常：

```
-keepclassmembers class * extends android.view.View {
    <init>(android.content.Context);
    <init>(android.content.Context, android.util.AttributeSet);
    public android.view.ViewGroup$LayoutParams generateDefaultLayoutParams();
}
-keepclassmembers class * extends android.view.ViewGroup$LayoutParams {
    <init>(int, int);
}
-keepnames class * extends android.view.View
-keepnames class *.*$LayoutParams
```

## TODO
- 单次测量的 Column 和 Row‘’
- 更友好的动画 API 
- ViewPager2
- 更多 Fragment 相关支持

## Thanks
- [Jetpack Compose](https://cs.android.com/androidx)
- [Hikage](https://github.com/BetterAndroid/Hikage)

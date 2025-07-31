# Hibari Material Components

这个模块提供了基于Material Design的UI组件的可运行封装，遵循Hibari框架的设计模式。

## 已实现的组件

### 基础组件

#### Button
```kotlin
Button(
    text = "Click Me",
    modifier = Modifier.fillMaxWidth(),
    icon = drawable
)
```

#### Text
```kotlin
Text(
    text = "Hello World",
    color = Color.Black,
    textSize = 16.sp,
    maxLines = 2,
    textAlign = TextAlign.Center
)
```

### 布局组件

#### Card
```kotlin
Card(
    modifier = Modifier.padding(16.dp),
    contentPadding = 16.dp,
    onClick = { /* Handle click */ }
) {
    // Card content
}
```

#### LazyColumn / LazyRow
```kotlin
LazyColumn {
    item {
        Text("Header")
    }
    
    items(items) { item ->
        Text(item.toString())
    }
    
    pos(5) { index ->
        Text("Item $index")
    }
}
```

### 输入组件

#### TextField
```kotlin
TextField(
    value = "Sample text",
    onValueChange = { newValue -> /* Handle change */ },
    label = "Input Field",
    placeholder = "Enter text here"
)
```

#### Switch
```kotlin
Switch(
    checked = true,
    onCheckedChange = { isChecked -> /* Handle change */ }
)
```

#### Checkbox
```kotlin
Checkbox(
    checked = false,
    onCheckedChange = { isChecked -> /* Handle change */ },
    text = "Checkbox label"
)
```

#### RadioButton
```kotlin
RadioButton(
    selected = true,
    onSelectionChanged = { isSelected -> /* Handle change */ },
    text = "Radio button label"
)
```

### 操作组件

#### FloatingActionButton
```kotlin
FloatingActionButton(
    onClick = { /* Handle click */ },
    icon = drawable,
    contentDescription = "Add item"
)
```

#### Chip
```kotlin
Chip(
    text = "Chip Label",
    onClick = { /* Handle click */ },
    selected = false
)
```

### 进度组件

#### LinearProgressIndicator
```kotlin
LinearProgressIndicator(
    progress = 0.7f,
    indeterminate = false
)
```

#### CircularProgressIndicator
```kotlin
CircularProgressIndicator(
    progress = 0.5f,
    indeterminate = false
)
```

### 导航组件

#### BottomNavigation
```kotlin
BottomNavigation(
    selectedItemId = 0,
    onItemSelected = { itemId -> /* Handle selection */ }
) {
    // Navigation items
}
```

#### TabRow
```kotlin
TabRow(
    selectedTabIndex = 0,
    onTabSelected = { index -> /* Handle tab selection */ }
) {
    // Tab items
}
```

### 反馈组件

#### AlertDialog
```kotlin
AlertDialog(
    onDismissRequest = { /* Handle dismiss */ },
    title = "Dialog Title",
    text = "Dialog message"
) {
    // Dialog content
}
```

#### Snackbar
```kotlin
Snackbar(
    message = "Operation completed",
    actionLabel = "Undo",
    onAction = { /* Handle action */ }
)
```

## 设计模式

所有组件都遵循以下设计模式：

1. **@Tunable注解**: 所有组件函数都使用@Tunable注解，支持Hibari的响应式更新
2. **Modifier参数**: 所有组件都接受Modifier参数，支持链式属性设置
3. **默认参数**: 提供合理的默认值，简化使用
4. **回调函数**: 使用lambda表达式处理用户交互
5. **内容函数**: 支持嵌套内容的组件使用@Tunable () -> Unit参数

## 使用示例

查看 `app/src/main/java/com/huanli233/hibari/sample/MaterialComponentsExample.kt` 文件获取完整的使用示例。

## 扩展

要添加新的Material组件，请遵循以下步骤：

1. 创建新的组件文件
2. 使用@Tunable注解
3. 使用Node函数包装Material组件
4. 使用viewClass指定对应的Android View类
5. 添加必要的属性修饰符
6. 提供合理的默认参数
7. 添加使用示例到示例文件中
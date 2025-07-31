# Hibari Material Components

This module provides Material Design components for the Hibari UI framework. All components are built using the `@Tunable` annotation and follow Hibari's declarative UI patterns.

## Components

### Text Components
- **Text**: Material TextView with customizable text properties
- **TextField**: Material TextInputLayout with TextInputEditText

### Button Components
- **Button**: Material Button with text and click handling
- **FloatingActionButton**: Material FAB with icon and click handling

### Selection Components
- **CheckBox**: Material CheckBox with text and state management
- **Switch**: Material Switch with text and state management
- **RadioButton**: Material RadioButton with text and state management

### Progress Components
- **CircularProgressBar**: Material CircularProgressIndicator
- **LinearProgressBar**: Material LinearProgressIndicator
- **Slider**: Material Slider with value range and change handling

### Layout Components
- **Card**: Material CardView with content and styling
- **LinearLayout**: Android LinearLayout with orientation
- **FrameLayout**: Android FrameLayout
- **RelativeLayout**: Android RelativeLayout
- **ScrollView**: Android ScrollView
- **HorizontalScrollView**: Android HorizontalScrollView

### Navigation Components
- **BottomNavigation**: Material BottomNavigationView
- **Toolbar**: Material Toolbar with title and menu

### Display Components
- **Chip**: Material Chip with optional close icon
- **Image**: ImageView with resource and scale type

## Usage

### Basic Import
```kotlin
import com.huanli233.hibari.material.*
```

### Simple Example
```kotlin
@Tunable
fun MyScreen() {
    LinearLayout {
        Text(
            text = "Hello World",
            textSize = 18.sp,
            modifier = Modifier.padding(16)
        )
        
        Button(
            text = "Click Me",
            onClick = {
                // Handle click
            },
            modifier = Modifier.padding(16)
        )
    }
}
```

### Form Example
```kotlin
@Tunable
fun LoginForm() {
    LinearLayout {
        TextField(
            value = "",
            hint = "Username",
            onValueChange = { value ->
                // Handle username change
            },
            modifier = Modifier.padding(16)
        )
        
        TextField(
            value = "",
            hint = "Password",
            modifier = Modifier.padding(16)
        )
        
        Button(
            text = "Login",
            onClick = {
                // Handle login
            },
            modifier = Modifier.padding(16)
        )
    }
}
```

### Card Example
```kotlin
@Tunable
fun ProductCard() {
    Card(
        cornerRadius = 8f,
        elevation = 4f,
        modifier = Modifier.padding(16)
    ) {
        LinearLayout {
            Image(
                src = R.drawable.product_image,
                modifier = Modifier.padding(16)
            )
            
            Text(
                text = "Product Name",
                textSize = 16.sp,
                modifier = Modifier.padding(16)
            )
            
            Button(
                text = "Add to Cart",
                onClick = {
                    // Handle add to cart
                },
                modifier = Modifier.padding(16)
            )
        }
    }
}
```

## Properties

### Common Properties
Most components support these common properties:
- `modifier`: Modifier for styling and layout
- `textColor`: Text color (Int)
- `textSize`: Text size (TextUnit)
- `textAlign`: Text alignment (TextAlign)

### Component-Specific Properties
Each component has its own specific properties. See the individual component files for detailed documentation.

## Styling

### Using Modifiers
```kotlin
Text(
    text = "Styled Text",
    modifier = Modifier
        .padding(16)
        .backgroundColor(Color.RED)
        .alpha(0.8f)
)
```

### Common Modifiers
- `padding(padding: Int)`: Add padding to all sides
- `paddingHorizontal(horizontal: Int)`: Add horizontal padding
- `paddingVertical(vertical: Int)`: Add vertical padding
- `backgroundColor(color: Int)`: Set background color
- `background(resource: Int)`: Set background resource
- `alpha(alpha: Float)`: Set alpha transparency
- `visibility(visibility: Int)`: Set visibility
- `enabled(enabled: Boolean)`: Set enabled state
- `clickable(clickable: Boolean)`: Set clickable state

## Event Handling

### Click Events
```kotlin
Button(
    text = "Click Me",
    onClick = {
        // Handle click event
    }
)
```

### Value Change Events
```kotlin
TextField(
    value = "",
    onValueChange = { newValue ->
        // Handle text change
    }
)
```

### State Change Events
```kotlin
CheckBox(
    checked = false,
    onCheckedChange = { isChecked ->
        // Handle checkbox state change
    }
)
```

## Examples

See `Examples.kt` for comprehensive examples of all components and their usage patterns.

## Dependencies

This module depends on:
- `hibari-foundation`: Core Hibari framework
- `hibari-ui`: UI components and modifiers
- `material`: Google Material Design components
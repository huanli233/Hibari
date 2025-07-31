package com.huanli233.hibari.material

import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.unit.TextUnit
import com.huanli233.hibari.ui.unit.sp

/**
 * Examples of using Material Design components in Hibari
 * 
 * These examples demonstrate how to use the Material components
 * in your Hibari UI code.
 */

@Tunable
fun SimpleFormExample() {
    LinearLayout {
        Text(
            text = "User Registration Form",
            textSize = 20.sp,
            modifier = Modifier.padding(16)
        )
        
        TextField(
            value = "",
            hint = "Enter your name",
            modifier = Modifier.padding(16)
        )
        
        TextField(
            value = "",
            hint = "Enter your email",
            modifier = Modifier.padding(16)
        )
        
        CheckBox(
            text = "I agree to the terms and conditions",
            modifier = Modifier.padding(16)
        )
        
        Button(
            text = "Submit",
            modifier = Modifier.padding(16),
            onClick = {
                // Handle form submission
            }
        )
    }
}

@Tunable
fun CardExample() {
    Card(
        modifier = Modifier.padding(16),
        cornerRadius = 12f,
        elevation = 8f
    ) {
        LinearLayout {
            Text(
                text = "Card Title",
                textSize = 18.sp,
                modifier = Modifier.padding(16)
            )
            
            Text(
                text = "This is the content of the card. It can contain any other components.",
                modifier = Modifier.padding(16)
            )
            
            Button(
                text = "Action",
                modifier = Modifier.padding(16),
                onClick = {
                    // Handle action
                }
            )
        }
    }
}

@Tunable
fun NavigationExample() {
    FrameLayout {
        // Main content
        LinearLayout {
            Text(
                text = "Main Content",
                modifier = Modifier.padding(16)
            )
            
            FloatingActionButton(
                icon = android.R.drawable.ic_input_add,
                onClick = {
                    // Handle FAB click
                }
            )
        }
        
        // Bottom navigation
        BottomNavigation(
            menuRes = android.R.menu.example_menu,
            selectedItemId = 0,
            onItemSelected = { itemId ->
                // Handle navigation
            }
        )
    }
}

@Tunable
fun ProgressExample() {
    LinearLayout {
        Text(
            text = "Loading Progress",
            modifier = Modifier.padding(16)
        )
        
        LinearProgressBar(
            progress = 75,
            modifier = Modifier.padding(16)
        )
        
        CircularProgressBar(
            progress = 50,
            modifier = Modifier.padding(16)
        )
        
        Slider(
            value = 25f,
            valueFrom = 0f,
            valueTo = 100f,
            onValueChange = { value ->
                // Handle slider change
            },
            modifier = Modifier.padding(16)
        )
    }
}

@Tunable
fun SelectionExample() {
    LinearLayout {
        Text(
            text = "Selection Controls",
            textSize = 18.sp,
            modifier = Modifier.padding(16)
        )
        
        CheckBox(
            text = "Checkbox option 1",
            modifier = Modifier.padding(8)
        )
        
        CheckBox(
            text = "Checkbox option 2",
            modifier = Modifier.padding(8)
        )
        
        Switch(
            text = "Switch option",
            modifier = Modifier.padding(8)
        )
        
        RadioButton(
            text = "Radio option 1",
            modifier = Modifier.padding(8)
        )
        
        RadioButton(
            text = "Radio option 2",
            modifier = Modifier.padding(8)
        )
    }
}

@Tunable
fun ChipExample() {
    LinearLayout {
        Text(
            text = "Chips",
            textSize = 18.sp,
            modifier = Modifier.padding(16)
        )
        
        HorizontalScrollView {
            LinearLayout(orientation = LinearLayout.HORIZONTAL) {
                Chip(
                    text = "Chip 1",
                    onClick = {
                        // Handle chip click
                    },
                    modifier = Modifier.padding(4)
                )
                
                Chip(
                    text = "Chip 2",
                    closeIconVisible = true,
                    onCloseClick = {
                        // Handle close click
                    },
                    modifier = Modifier.padding(4)
                )
                
                Chip(
                    text = "Chip 3",
                    modifier = Modifier.padding(4)
                )
            }
        }
    }
}
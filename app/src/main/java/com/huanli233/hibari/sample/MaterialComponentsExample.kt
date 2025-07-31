package com.huanli233.hibari.sample

import com.huanli233.hibari.foundation.Linear
import com.huanli233.hibari.foundation.Spacer
import com.huanli233.hibari.foundation.attributes.fillMaxWidth
import com.huanli233.hibari.foundation.attributes.padding
import com.huanli233.hibari.material.*
import com.huanli233.hibari.recyclerview.LazyColumn
import com.huanli233.hibari.recyclerview.items
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.unit.Dp

@Tunable
fun MaterialComponentsExample() {
    LazyColumn {
        item {
            Text(
                text = "Material Components Demo",
                modifier = Modifier.padding(16.dp)
            )
        }
        
        item {
            Card(
                modifier = Modifier.padding(16.dp),
                contentPadding = 16.dp
            ) {
                Linear {
                    Text(text = "Card Component")
                    Spacer(height = 8.dp)
                    Button(
                        text = "Click Me",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        item {
            TextField(
                value = "Sample text",
                onValueChange = { /* Handle text change */ },
                modifier = Modifier.padding(16.dp),
                label = "Input Field"
            )
        }
        
        item {
            Linear {
                Switch(
                    checked = true,
                    onCheckedChange = { /* Handle switch change */ }
                )
                Spacer(width = 16.dp)
                Text(text = "Switch Component")
            }
        }
        
        item {
            Linear {
                Checkbox(
                    checked = false,
                    onCheckedChange = { /* Handle checkbox change */ },
                    text = "Checkbox Component"
                )
            }
        }
        
        item {
            Linear {
                RadioButton(
                    selected = true,
                    onSelectionChanged = { /* Handle radio change */ },
                    text = "Radio Button Component"
                )
            }
        }
        
        item {
            FloatingActionButton(
                onClick = { /* Handle FAB click */ },
                modifier = Modifier.padding(16.dp)
            )
        }
        
        item {
            Linear {
                Chip(
                    text = "Chip 1",
                    onClick = { /* Handle chip click */ }
                )
                Spacer(width = 8.dp)
                Chip(
                    text = "Chip 2",
                    onClick = { /* Handle chip click */ }
                )
            }
        }
        
        item {
            LinearProgressIndicator(
                progress = 0.7f,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        item {
            CircularProgressIndicator(
                progress = 0.5f,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        items(listOf("Tab 1", "Tab 2", "Tab 3")) { tabText ->
            Text(
                text = tabText,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
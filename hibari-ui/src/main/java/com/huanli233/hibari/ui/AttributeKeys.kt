package com.huanli233.hibari.ui

import android.view.View

object AttributeKeys {

    val viewClass = AttributeKey<Class<out View>>("viewClass")
    val attributeSet = AttributeKey<Int>("attributeSet")
    val id = AttributeKey<String>("id")
    val tag = AttributeKey<String>("tag")

}

object TextViewAttributeKeys {

    val text = AttributeKey<String>("text")

}
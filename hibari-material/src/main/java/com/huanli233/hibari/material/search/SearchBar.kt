package com.huanli233.hibari.material.search

import android.graphics.drawable.Drawable
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun SearchBar(
    modifier: Modifier = Modifier,
    text: CharSequence? = null,
    hint: CharSequence? = null,
    navigationIcon: Any? = null,
    onNavigationClick: (() -> Unit)? = null,
) {
    Node(
        modifier = modifier
            .viewClass(SearchBar::class.java)
            .thenViewAttributeIfNotNull<SearchBar, CharSequence>(uniqueKey, text) { this.setText(it) }
            .thenViewAttributeIfNotNull<SearchBar, CharSequence>(uniqueKey, hint) { this.hint = it }
            .thenViewAttributeIfNotNull<SearchBar, Any>(uniqueKey, navigationIcon) {
                when (it) {
                    is Int -> this.setNavigationIcon(it)
                    is Drawable -> this.navigationIcon = it
                }
            }
            .thenViewAttributeIfNotNull<SearchBar, () -> Unit>(uniqueKey, onNavigationClick) { setNavigationOnClickListener { it() } }
    )
}

@Tunable
fun SearchView(
    modifier: Modifier = Modifier,
    text: CharSequence? = null,
    hint: CharSequence? = null,
    content: @Tunable SearchViewScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(SearchView::class.java)
            .thenViewAttributeIfNotNull<SearchView, CharSequence>(uniqueKey, text) { this.setText(it) }
            .thenViewAttributeIfNotNull<SearchView, CharSequence>(uniqueKey, hint) { this.hint = it },
        content = {
            SearchViewScopeInstance.content()
        }
    )
}

interface SearchViewScope
internal object SearchViewScopeInstance : SearchViewScope
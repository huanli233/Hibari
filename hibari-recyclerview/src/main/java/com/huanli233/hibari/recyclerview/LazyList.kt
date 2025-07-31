package com.huanli233.hibari.recyclerview

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.layout.LayoutScopeMarker
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.currentTuner
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.ref
import com.huanli233.hibari.ui.viewClass

@LayoutScopeMarker
interface LazyListScope {
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Tunable () -> Unit
    )

    fun pos(
        count: Int,
        key: (index: Int) -> Any? = { it },
        contentType: (index: Int) -> Any? = { null },
        content: @Tunable (index: Int) -> Unit
    )

    fun <T> items(
        items: List<T>,
        key: (item: T) -> Any? = { it },
        contentType: (item: T) -> Any? = { null },
        content: @Tunable (item: T) -> Unit
    )
}

internal class LazyListScopeImpl : LazyListScope {

    private val _items = mutableListOf<LazyListItem>()
    val items: List<LazyListItem> get() = _items

    fun reset() {
        _items.clear()
    }

    override fun item(key: Any?, contentType: Any?, content: @Tunable () -> Unit) {
        _items.add(
            LazyListItem(
                key = key,
                contentType = contentType,
                content = content
            )
        )
    }

    override fun pos(
        count: Int,
        key: (index: Int) -> Any?,
        contentType: (index: Int) -> Any?,
        content: @Tunable (index: Int) -> Unit
    ) {
        for (index in 0 until count) {
            _items.add(
                LazyListItem(
                    key = key(index),
                    contentType = contentType(index),
                    content = { content(index) }
                )
            )
        }
    }

    override fun <T> items(
        items: List<T>,
        key: (item: T) -> Any?,
        contentType: (item: T) -> Any?,
        content: @Tunable (item: T) -> Unit
    ) {
        items.forEach { item ->
            _items.add(
                LazyListItem(
                    key = key(item),
                    contentType = contentType(item),
                    content = { content(item) }
                )
            )
        }
    }
}

@Tunable
fun LazyRow(
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    content: LazyListScope.() -> Unit,
) {
    val parentTunation = currentTuner.tunation
    val adapter = remember { HibariAdapter(parentTunation) }
    val scope = LazyListScopeImpl().apply(content)
    adapter.submitList(scope.items)

    Node(
        modifier = modifier
            .viewClass(RecyclerView::class.java)
            .ref {
                if (it is RecyclerView) {
                    it.layoutManager = LinearLayoutManager(it.context, RecyclerView.HORIZONTAL, reverseLayout)
                    it.adapter = adapter
                }
            }
    )
}

@Tunable
fun LazyColumn(
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    content: LazyListScope.() -> Unit,
) {
    val parentTunation = currentTuner.tunation
    val adapter = remember { HibariAdapter(parentTunation) }
    val scope = LazyListScopeImpl().apply(content)
    adapter.submitList(scope.items)

    Node(
        modifier = modifier
            .viewClass(RecyclerView::class.java)
            .ref {
                if (it is RecyclerView) {
                    it.layoutManager = LinearLayoutManager(it.context, RecyclerView.VERTICAL, reverseLayout)
                    it.adapter = adapter
                }
            }
    )
}
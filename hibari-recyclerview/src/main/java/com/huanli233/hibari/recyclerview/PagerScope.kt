package com.huanli233.hibari.recyclerview

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.layout.LayoutScopeMarker
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.currentTuner
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.ref
import com.huanli233.hibari.ui.viewClass

@LayoutScopeMarker
interface PagerScope {
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Tunable () -> Unit
    )

    fun items(
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

internal class PagerScopeImpl : PagerScope {

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

    override fun items(
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
fun Pager(
    modifier: Modifier = Modifier,
    orientation: Int = ViewPager2.ORIENTATION_HORIZONTAL,
    content: PagerScope.() -> Unit,
) {
    val parentTunation = currentTuner.tunation
    val adapter = remember { HibariAdapter(parentTunation) }
    val scope = PagerScopeImpl().apply(content)
    adapter.submitList(scope.items)

    Node(
        modifier = modifier
            .viewClass(ViewPager2::class.java)
            .ref { view ->
                if (view is ViewPager2) {
                    view.orientation = orientation
                    view.adapter = adapter
                }
            }
    )
}


@Tunable
fun <T> FragmentPager(
    modifier: Modifier = Modifier,
    activity: FragmentActivity,
    items: List<T>,
    orientation: Int = ViewPager2.ORIENTATION_HORIZONTAL,
    key: (item: T) -> Long = { it.hashCode().toLong() },
    fragmentFactory: (item: T) -> Fragment
) {
    val adapter = remember(items) {
        object : FragmentStateAdapter(activity) {
            override fun getItemCount(): Int = items.size

            override fun createFragment(position: Int): Fragment = fragmentFactory(items[position])

            override fun getItemId(position: Int): Long = key(items[position])

            override fun containsItem(itemId: Long): Boolean = items.any { key(it) == itemId }
        }
    }

    Node(
        modifier = modifier
            .viewClass(ViewPager2::class.java)
            .ref { view ->
                if (view is ViewPager2) {
                    view.orientation = orientation
                    if (view.adapter !== adapter) {
                        view.adapter = adapter
                    }
                }
            }
    )
}
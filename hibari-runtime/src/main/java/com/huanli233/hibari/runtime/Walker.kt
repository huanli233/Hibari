package com.huanli233.hibari.runtime

class Walker {

    private val ends = ArrayDeque<Int>()

    fun path() = ends.joinToString("-")

    fun start(endKey: Int) {
        ends.addLast(endKey)
    }

    fun end() {
        ends.removeLast()
    }

    fun clear() {
        ends.clear()
    }

}
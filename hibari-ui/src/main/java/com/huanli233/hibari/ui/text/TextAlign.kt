package com.huanli233.hibari.ui.text

class TextAlign internal constructor(internal val value: Int) {

    override fun toString(): String {
        return when (this) {
            Left -> "Left"
            Right -> "Right"
            Center -> "Center"
            Start -> "Start"
            End -> "End"
            Unspecified -> "Unspecified"
            else -> "Invalid"
        }
    }

    companion object {
        /** Align the text on the left edge of the container. */
        val Left = TextAlign(1)

        /** Align the text on the right edge of the container. */
        val Right = TextAlign(2)

        /** Align the text in the center of the container. */
        val Center = TextAlign(3)

        /**
         * Align the text on the leading edge of the container.
         *
         * For Left to Right text ([ResolvedTextDirection.Ltr]), this is the left edge.
         *
         * For Right to Left text ([ResolvedTextDirection.Rtl]), like Arabic, this is the right edge.
         */
        val Start = TextAlign(5)

        /**
         * Align the text on the trailing edge of the container.
         *
         * For Left to Right text ([ResolvedTextDirection.Ltr]), this is the right edge.
         *
         * For Right to Left text ([ResolvedTextDirection.Rtl]), like Arabic, this is the left edge.
         */
        val End = TextAlign(6)

        /**
         * Return a list containing all possible values of TextAlign.
         */
        fun values(): List<TextAlign> = listOf(Left, Right, Center, Start, End)

        /**
         * This represents an unset value, a usual replacement for "null" when a primitive value
         * is desired.
         */
        val Unspecified = TextAlign(Int.MIN_VALUE)
    }
}
package kotlin.swing

import java.awt.Dimension
import java.awt.Component
import javax.swing.JComponent

/**
 * Returns the width of the dimension or zero if its null
 */
fun Dimension?.width(): Int {
    return if (this == null) 0 else this.width
}

/**
 * Returns the height of the dimension or zero if its null
 */
fun Dimension?.height(): Int {
    return if (this == null) 0 else this.height
}

/**
 * Returns a new dimension with the same height and the given width
 */
fun Dimension?.width(newValue: Int): Dimension {
    return Dimension(newValue, height())
}
/**
 * Returns a new dimension with the same width and the given height
 */
fun Dimension?.height(newValue: Int): Dimension {
    return Dimension(width(), newValue)
}

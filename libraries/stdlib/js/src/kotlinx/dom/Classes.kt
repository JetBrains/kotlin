/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
package kotlinx.dom

import kotlinx.browser.PLEASE_USE_KOTLINX_BROWSER_INSTEAD
import org.w3c.dom.*

/** Returns true if the element has the given CSS class style in its 'class' attribute */
@SinceKotlin("1.4")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public fun Element.hasClass(cssClass: String): Boolean = className.matches("""(^|.*\s+)$cssClass($|\s+.*)""".toRegex())

/**
 * Adds CSS class to element. Has no effect if all specified classes are already in class attribute of the element
 *
 * @return true if at least one class has been added
 */
@SinceKotlin("1.4")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public fun Element.addClass(vararg cssClasses: String): Boolean {
    val missingClasses = cssClasses.filterNot { hasClass(it) }
    if (missingClasses.isNotEmpty()) {
        val presentClasses = className.trim()
        className = buildString {
            append(presentClasses)
            if (!presentClasses.isEmpty()) {
                append(" ")
            }
            missingClasses.joinTo(this, " ")
        }
        return true
    }

    return false
}

/**
 * Removes all [cssClasses] from element. Has no effect if all specified classes are missing in class attribute of the element
 *
 * @return true if at least one class has been removed
 */
@SinceKotlin("1.4")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public fun Element.removeClass(vararg cssClasses: String): Boolean {
    if (cssClasses.any { hasClass(it) }) {
        val toBeRemoved = cssClasses.toSet()
        className = className.trim().split("\\s+".toRegex()).filter { it !in toBeRemoved }.joinToString(" ")
        return true
    }

    return false
}

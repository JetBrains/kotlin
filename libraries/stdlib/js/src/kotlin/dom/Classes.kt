/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.dom

import org.w3c.dom.Element
import kotlin.internal.LowPriorityInOverloadResolution
import kotlinx.dom.addClass as newAddClass
import kotlinx.dom.hasClass as newHasClass
import kotlinx.dom.removeClass as newRemoveClass

/** Returns true if the element has the given CSS class style in its 'class' attribute */
@LowPriorityInOverloadResolution
@Deprecated(
    message = "This API is moved to another package, use 'kotlinx.dom.hasClass' instead.",
    replaceWith = ReplaceWith("this.hasClass(cssClass)", "kotlinx.dom.hasClass")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
public inline fun Element.hasClass(cssClass: String): Boolean = this.newHasClass(cssClass)

/**
 * Adds CSS class to element. Has no effect if all specified classes are already in class attribute of the element
 *
 * @return true if at least one class has been added
 */
@LowPriorityInOverloadResolution
@Deprecated(
    message = "This API is moved to another package, use 'kotlinx.dom.addClass' instead.",
    replaceWith = ReplaceWith("this.addClass(cssClasses)", "kotlinx.dom.addClass")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
public inline fun Element.addClass(vararg cssClasses: String): Boolean = this.newAddClass(*cssClasses)

/**
 * Removes all [cssClasses] from element. Has no effect if all specified classes are missing in class attribute of the element
 *
 * @return true if at least one class has been removed
 */
@LowPriorityInOverloadResolution
@Deprecated(
    message = "This API is moved to another package, use 'kotlinx.dom.removeClass' instead.",
    replaceWith = ReplaceWith("this.removeClass(cssClasses)", "kotlinx.dom.removeClass")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
public inline fun Element.removeClass(vararg cssClasses: String): Boolean = this.newRemoveClass(*cssClasses)
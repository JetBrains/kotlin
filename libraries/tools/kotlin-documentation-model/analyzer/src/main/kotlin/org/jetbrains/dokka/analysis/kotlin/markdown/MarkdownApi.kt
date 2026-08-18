/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.markdown

import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.TagWrapper

/**
 * This constant is generally used in [CustomDocTag.name] as an indicator that a [CustomDocTag] is a "root" tag
 * (i.e a container for other tags), which can then be used in [TagWrapper]. However, it might have
 * other uses around the same area.
 *
 * The use and the need for this constant will be revisited in the future, see
 * [#3404](https://github.com/Kotlin/dokka/issues/3404) for more details.
 *
 * The current name and value are chosen for compatibility reasons, because before this constant
 * was introduced, a different constant named `MARKDOWN_FILE` was used, but it's no longer available.
 */
public const val MARKDOWN_ELEMENT_FILE_NAME: String = "MARKDOWN_FILE" // see #3366 and #3404 for more context

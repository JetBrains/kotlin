/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.junit

@Deprecated(
    "Use 'Test' from kotlin.test package",
    replaceWith = ReplaceWith("kotlin.test.Test", "kotlin.test.Test"),
    level = DeprecationLevel.ERROR
)
actual typealias Test = kotlin.test.Test

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
package kotlinx.browser

import org.w3c.dom.*

@SinceKotlin("1.4")
@Deprecated(
    message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD,
    level = DeprecationLevel.WARNING
)
public external val window: Window

@SinceKotlin("1.4")
@Deprecated(
    message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD,
    level = DeprecationLevel.WARNING
)
public external val document: Document

@SinceKotlin("1.4")
@Deprecated(
    message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD,
    level = DeprecationLevel.WARNING
)
public external val localStorage: Storage

@SinceKotlin("1.4")
@Deprecated(
    message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD,
    level = DeprecationLevel.WARNING
)
public external val sessionStorage: Storage


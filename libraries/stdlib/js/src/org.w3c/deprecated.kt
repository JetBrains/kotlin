/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.w3c.dom

@Deprecated("Use UnionMessagePortOrWindowProxy instead.", ReplaceWith("UnionMessagePortOrWindowProxy"))
typealias UnionMessagePortOrWindow = UnionMessagePortOrWindowProxy

@Deprecated("Use `as` instead.", ReplaceWith("`as`"))
var HTMLLinkElement.as_
    get() = `as`
    set(value) {
        `as` = value
    }

@Deprecated("Use `is` instead.", ReplaceWith("`is`"))
var ElementCreationOptions.is_
    get() = `is`
    set(value) {
        `is` = value
    }
/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.w3c.dom

@Deprecated("Use UnionMessagePortOrWindowProxy instead.", ReplaceWith("UnionMessagePortOrWindowProxy"))
public typealias UnionMessagePortOrWindow = UnionMessagePortOrWindowProxy

@Deprecated("Use `as` instead.", ReplaceWith("`as`"))
public var HTMLLinkElement.as_: org.w3c.fetch.RequestDestination
    get() = `as`
    set(value) {
        `as` = value
    }

@Deprecated("Use `is` instead.", ReplaceWith("`is`"))
public var ElementCreationOptions.is_: String?
    get() = `is`
    set(value) {
        `is` = value
    }
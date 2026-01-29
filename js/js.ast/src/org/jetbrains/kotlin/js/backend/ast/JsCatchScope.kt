// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package org.jetbrains.kotlin.js.backend.ast

/**
 * A special scope used only for catch blocks. It only holds a single symbol:
 * the catch argument's name.
 */
class JsCatchScope(
    parent: JsScope,
    private val assignable: JsAssignable
) : JsDeclarationScope(parent, "Catch scope", true) {
    private val names get() = assignable.names

    override fun declareName(identifier: String): JsName {
        // Declare into parent scope!
        return parent.declareName(identifier)
    }

    override fun hasOwnName(name: String): Boolean {
        return findOwnName(name) != null
    }

    fun copy(): JsCatchScope {
        return JsCatchScope(parent, assignable)
    }

    override fun findOwnName(ident: String): JsName? {
        return names.firstOrNull { it.ident == ident }
    }
}

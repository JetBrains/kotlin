/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.regex

internal class NamedGroup(val name: String) : SpecialToken() {

    override fun toString() = "NamedGroup(name=$name)"

    override val type: Type = SpecialToken.Type.NAMED_GROUP
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import java.io.Serializable

abstract class Lambda(private val arity: Int) : FunctionBase, Serializable {
    override fun getArity(): Int = arity

    override fun toString(): String = Reflection.renderLambdaToString(this)
}

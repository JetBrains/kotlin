/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

public abstract class Lambda(private val arity: Int) : FunctionBase {
    override fun getArity() = arity

    override fun toString() = Reflection.renderLambdaToString(this)
}

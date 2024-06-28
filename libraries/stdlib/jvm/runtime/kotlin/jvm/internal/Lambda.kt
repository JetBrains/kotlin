/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import java.io.Serializable

public abstract class Lambda<out R>(override val arity: Int) : FunctionBase<R>, Serializable {
    override fun toString(): String = Reflection.renderLambdaToString(this)
}

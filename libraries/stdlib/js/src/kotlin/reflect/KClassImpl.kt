/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.js.internal

import kotlin.reflect.*

internal abstract class KClassImpl<T : Any> : KClass<T> {
    internal abstract val jClass: JsClass<T>

    override val qualifiedName: String?
        get() = null

    @ExperimentalStdlibApi
    @SinceKotlin("2.2")
    override val isInterface: Boolean
        get() = jClass.asDynamic().`$metadata$`.unsafeCast<Metadata?>()?.kind == METADATA_KIND_INTERFACE

    override fun equals(other: Any?): Boolean {
        return when (other) {
            // NothingKClassImpl doesn't provide the jClass property; therefore, process it separately.
            // This can't be NothingKClassImpl because it overload equals.
            is NothingKClassImpl -> false
            is KClassImpl<*> -> jClass == other.jClass
            else -> false
        }
    }

    // TODO: use FQN
    override fun hashCode(): Int = simpleName?.hashCode() ?: 0

    override fun toString(): String {
        // TODO: use FQN
        return "class $simpleName"
    }
}

internal class SimpleKClassImpl<T : Any>(override val jClass: JsClass<T>) : KClassImpl<T>() {
    override val simpleName: String? = jClass.asDynamic().`$metadata$`?.simpleName.unsafeCast<String?>()

    override fun isInstance(value: Any?): Boolean {
        return jsIsType(value, jClass)
    }
}

internal class PrimitiveKClassImpl<T : Any>(
    override val jClass: JsClass<T>,
    private val givenSimpleName: String,
    private val isInstanceFunction: (Any?) -> Boolean
) : KClassImpl<T>() {

    @ExperimentalStdlibApi
    @SinceKotlin("2.2")
    override val isInterface: Boolean
        get() = false

    override fun equals(other: Any?): Boolean {
        if (other !is PrimitiveKClassImpl<*>) return false
        return super.equals(other) && givenSimpleName == other.givenSimpleName
    }

    override val simpleName: String? get() = givenSimpleName

    override fun isInstance(value: Any?): Boolean {
        return isInstanceFunction(value)
    }
}

internal object NothingKClassImpl : KClassImpl<Nothing>() {
    override val simpleName: String = "Nothing"

    override fun isInstance(value: Any?): Boolean = false

    @ExperimentalStdlibApi
    @SinceKotlin("2.2")
    override val isInterface: Boolean
        get() = false

    override val jClass: JsClass<Nothing>
        get() = throw UnsupportedOperationException("There's no native JS class for Nothing type")

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 0
}

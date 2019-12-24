/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.js.internal

import kotlin.reflect.*

internal abstract class KClassImpl<T : Any>(
    internal open val jClass: JsClass<T>
) : KClass<T> {

    override val qualifiedName: String?
        get() = TODO()

    override fun equals(other: Any?): Boolean {
        return other is KClassImpl<*> && jClass == other.jClass
    }

    // TODO: use FQN
    override fun hashCode(): Int = simpleName?.hashCode() ?: 0

    override fun toString(): String {
        // TODO: use FQN
        return "class $simpleName"
    }
}

internal class SimpleKClassImpl<T : Any>(jClass: JsClass<T>) : KClassImpl<T>(jClass) {
    override val simpleName: String? = jClass.asDynamic().`$metadata$`?.simpleName.unsafeCast<String?>()

    override fun isInstance(value: Any?): Boolean {
        return jsIsType(value, jClass)
    }
}

internal class PrimitiveKClassImpl<T : Any>(
    jClass: JsClass<T>,
    private val givenSimpleName: String,
    private val isInstanceFunction: (Any?) -> Boolean
) : KClassImpl<T>(jClass) {
    override fun equals(other: Any?): Boolean {
        if (other !is PrimitiveKClassImpl<*>) return false
        return super.equals(other) && givenSimpleName == other.givenSimpleName
    }

    override val simpleName: String? get() = givenSimpleName

    override fun isInstance(value: Any?): Boolean {
        return isInstanceFunction(value)
    }
}

internal object NothingKClassImpl : KClassImpl<Nothing>(js("Object")) {
    override val simpleName: String = "Nothing"

    override fun isInstance(value: Any?): Boolean = false

    override val jClass: JsClass<Nothing>
        get() = throw UnsupportedOperationException("There's no native JS class for Nothing type")

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 0
}
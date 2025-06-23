/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.js.internal

import kotlin.reflect.*

internal abstract class KClassImpl<T : Any> : KClass<T> {
    internal abstract val jClass: JsClass<T>

    override val qualifiedName: String?
        get() = TODO()

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

    @ExperimentalStdlibApi
    @SinceKotlin("2.2")
    override val isInterface: Boolean
        get() = false

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

internal class InterfaceKClass<T : Any>(val interfaceId: Int) : KClassImpl<T>() {
    override val jClass: JsClass<T> = initJsClass()

    private fun initJsClass(): JsClass<T> {
        // kotlinx-serialization relies on the presence of JsClass in KClass instances that represent interfaces,
        // as well as `$metadata$`:
        // https://github.com/Kotlin/kotlinx.serialization/blob/4667a1891a925dc9e3e10490c274a875b0be4da6/core/jsMain/src/kotlinx/serialization/internal/Platform.kt#L81
        // This is why we have to generate this, even though the concept of interfaces having a "constructor" is senseless.
        //
        // Hopefully, by the time you are reading this, kotlinx-serialization has already removed that hack, but in any case,
        // we don't want to break the clients of its older versions who decided to upgrade their Kotlin.
        val constructor = js("function InterfaceConstructor() {}")
        val interfaceName = simpleName ?: "Interface$interfaceId"

        // Note: we cannot just do `constructor.name = interfaceName` here, because function names are not writable.
        js("Object.defineProperty(constructor, 'name', {value: interfaceName})")

        constructor.`$metadata$` = createMetadata(METADATA_KIND_INTERFACE, interfaceName, VOID, VOID, VOID, VOID)
        return constructor.unsafeCast<JsClass<T>>()
    }

    override val simpleName: String?
        get() = getInterfaceIdMetadata(interfaceId)?.name

    override val qualifiedName: String
        get() = throw UnsupportedOperationException("InterfaceKClass doesn't support qualifiedName")

    @ExperimentalStdlibApi
    @SinceKotlin("2.2")
    override val isInterface: Boolean
        get() = true

    override fun isInstance(value: Any?): Boolean =
        value?.let { isInterfaceImpl(it, interfaceId) } ?: false

    override fun equals(other: Any?): Boolean = interfaceId == (other as? InterfaceKClass<*>)?.interfaceId

    override fun hashCode(): Int = interfaceId

    override fun toString(): String = "class $simpleName"
}

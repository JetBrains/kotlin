/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.internal.JvmBuiltin

package kotlin.reflect

/**
 * Represents a class and provides introspection capabilities.
 * Instances of this class are obtainable by the `::class` syntax.
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/reflection.html#class-references)
 * for more information.
 *
 * @param T the type of the class.
 */
public expect interface KClass<T : Any> : KClassifier {
    /**
     * The simple name of the class as it was declared in the source code,
     * or `null` if the class has no name (if, for example, it is a class of an anonymous object).
     */
    public val simpleName: String?

    /**
     * The fully qualified dot-separated name of the class,
     * or `null` if the class is local or a class of an anonymous object.
     *
     * This property is currently not supported in Kotlin/JS and Kotlin/Wasm.
     * In Kotlin/Wasm, however, it could be enabled using compiler options.
     * Please refer to the documentation for these targets for more details.
     */
    public val qualifiedName: String?

    /**
     * Returns `true` if [value] is an instance of this class on a given platform.
     */
    @SinceKotlin("1.1")
    public fun isInstance(value: Any?): Boolean

    override fun equals(other: Any?): Boolean // KT-24971

    override fun hashCode(): Int // KT-24971
}

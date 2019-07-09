/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

/**
 * Instructs the Kotlin compiler to treat annotated Java class as pure implementation of given Kotlin interface.
 * "Pure" means here that each type parameter of class becomes non-platform type argument of that interface.
 *
 * Example:
 *
 * ```java
 * class MyList<T> extends AbstractList<T> { ... }
 * ```
 *
 * Methods defined in `MyList<T>` use `T` as platform, i.e. it's possible to perform unsafe operation in Kotlin:
 *
 * ```kotlin
 *  MyList<Int>().add(null) // compiles
 * ```
 *
 * ```java
 * @PurelyImplements("kotlin.collections.MutableList")
 * class MyPureList<T> extends AbstractList<T> { ... }
 * ```
 *
 * Methods defined in `MyPureList<T>` overriding methods in `MutableList` use `T` as non-platform types:
 *
 * ```kotlin
 *  MyPureList<Int>().add(null) // Error
 *  MyPureList<Int?>().add(null) // Ok
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class PurelyImplements(val value: String)

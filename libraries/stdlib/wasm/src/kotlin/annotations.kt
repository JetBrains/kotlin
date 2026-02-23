/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Forces a top-level property to be initialized eagerly, opposed to lazily on the first access to file and/or property.
 */
@ExperimentalStdlibApi
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
@SinceKotlin("1.6")
@Deprecated("This annotation is a temporal migration assistance and may be removed in the future releases, please consider filing an issue about the case where it is needed")
public annotation class EagerInitialization

/**
 * TODO proper doc comment
 * TODO arguments
 * Marks an external interface as a WIT (WebAssembly Interface Type) interface.
 *
 * Classes and interfaces annotated with `@WitInterface` are treated as WIT interface
 * definitions by the Kotlin/Wasm compiler backend.
 *
 * @property name The WIT interface name. If not specified, the Kotlin name is used.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("2.4")
public annotation class WitInterface(val witName: String = "")

/**
 * TODO proper doc comment
 * TODO arguments
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("2.4")
public annotation class WitImport()

/**
 * TODO proper doc comment
 * TODO arguments
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("2.4")
public annotation class WitExport()

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * This annotation is present on any class file produced by the Kotlin compiler and is read by the compiler and reflection.
 * Parameters have very short names on purpose: these names appear in the generated class files, and we'd like to reduce their size.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@SinceKotlin("1.3")
public annotation class Metadata(
    /**
     * A kind of the metadata this annotation encodes. Kotlin compiler recognizes the following kinds (see KotlinClassHeader.Kind):
     *
     * 1 Class
     * 2 File
     * 3 Synthetic class
     * 4 Multi-file class facade
     * 5 Multi-file class part
     *
     * The class file with a kind not listed here is treated as a non-Kotlin file.
     */
    val k: Int = 1,
    /**
     * The version of the metadata provided in the arguments of this annotation.
     */
    val mv: IntArray = [],
    /**
     * The version of the bytecode interface (naming conventions, signatures) of the class file annotated with this annotation.
     */
    val bv: IntArray = [],
    /**
     * Metadata in a custom format. The format may be different (or even absent) for different kinds.
     */
    val d1: Array<String> = [],
    /**
     * An addition to [d1]: array of strings which occur in metadata, written in plain text so that strings already present
     * in the constant pool are reused. These strings may be then indexed in the metadata by an integer index in this array.
     */
    val d2: Array<String> = [],
    /**
     * An extra string. For a multi-file part class, internal name of the facade class.
     */
    val xs: String = "",
    /**
     * Fully qualified name of the package this class is located in, from Kotlin's point of view, or empty string if this name
     * does not differ from the JVM's package FQ name. These names can be different in case the [JvmPackageName] annotation is used.
     * Note that this information is also stored in the corresponding module's `.kotlin_module` file.
     */
    @SinceKotlin("1.2")
    val pn: String = "",
    /**
     * An extra int. Bits of this number represent the following flags:
     *
     * 0 - this is a multi-file class facade or part, compiled with `-Xmultifile-parts-inherit`.
     * 1 - this class file is compiled by a pre-release version of Kotlin and is not visible to release versions.
     * 2 - this class file is a compiled Kotlin script source file (.kts).
     */
    @SinceKotlin("1.1")
    val xi: Int = 0
)

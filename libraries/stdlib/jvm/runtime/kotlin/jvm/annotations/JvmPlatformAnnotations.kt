/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

import kotlin.reflect.KClass

/**
 * Instructs the Kotlin compiler to generate overloads for this function that substitute default parameter values.
 *
 * If a method has N parameters and M of which have default values, M overloads are generated: the first one
 * takes N-1 parameters (all but the last one that takes a default value), the second takes N-2 parameters, and so on.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public actual annotation class JvmOverloads

/**
 * Specifies that an additional static method needs to be generated from this element if it's a function.
 * If this element is a property, additional static getter/setter methods should be generated.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#static-methods)
 * for more information.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public actual annotation class JvmStatic

/**
 * Specifies the name for the Java class or method which is generated from this element.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#handling-signature-clashes-with-jvmname)
 * for more information.
 * @property name the name of the element.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public actual annotation class JvmName(actual val name: String)

/**
 * Instructs the Kotlin compiler to generate a multifile class with top-level functions and properties declared in this file as one of its parts.
 * Name of the corresponding multifile class is provided by the [JvmName] annotation.
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public actual annotation class JvmMultifileClass

/**
 * Changes the fully qualified name of the JVM package of the .class file generated from this file.
 * This does not affect the way Kotlin clients will see the declarations in this file, but Java clients and other JVM language clients
 * will see the class file as if it was declared in the specified package.
 * If a file is annotated with this annotation, it can only have function, property and typealias declarations, but no classes.
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@SinceKotlin("1.2")
internal actual annotation class JvmPackageName(actual val name: String)

/**
 * Sets `ACC_SYNTHETIC` flag on the annotated target in the Java bytecode.
 *
 * Synthetic targets become inaccessible for Java sources at compile time while still being accessible for Kotlin sources.
 * Marking target as synthetic is a binary compatible change, already compiled Java code will be able to access such target.
 *
 * This annotation is intended for *rare cases* when API designer needs to hide Kotlin-specific target from Java API
 * while keeping it a part of Kotlin API so the resulting API is idiomatic for both languages.
 */
@Target(AnnotationTarget.FILE, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
public actual annotation class JvmSynthetic

/**
 * This annotation indicates what exceptions should be declared by a function when compiled to a JVM method.
 *
 * Example:
 *
 * ```
 * @Throws(IOException::class)
 * fun readFile(name: String): String {...}
 * ```
 *
 * will be translated to
 *
 * ```
 * String readFile(String name) throws IOException {...}
 * ```
 *
 * @property exceptionClasses the list of checked exception classes that may be thrown by the function.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
public annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

/**
 * This annotation marks Kotlin `expect` declarations that are implicitly actualized by Java.
 *
 * # Safety Risks
 *
 * Implicit actualization bypasses safety features, potentially leading to errors or unexpected behavior. If you use this annotation, some
 * of the expect-actual invariants are not checked.
 *
 * Use this annotation only as a last resort. The annotation might stop working in future Kotlin versions without prior notice.
 *
 * If you use this annotation, consider describing your use cases in [KT-58545](https://youtrack.jetbrains.com/issue/KT-58545) comments.
 *
 * # Migration
 *
 * Alternatives:
 * 1. Rewrite the code using explicit `actual typealias`. Unfortunately, it requires you to move your expect declarations into another package.
 *    Refer to [KT-58545](https://youtrack.jetbrains.com/issue/KT-58545) for a more detailed migration example.
 * 2. Use `kotlin.jvm.KotlinActual` experimental feature. See https://youtrack.jetbrains.com/issue/KT-67202
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
@ExperimentalMultiplatform
@MustBeDocumented
@SinceKotlin("1.9")
@Deprecated(
    "Please migrate to kotlin.jvm.KotlinActual in kotlin-annotations-jvm. " +
            "ImplicitlyActualizedByJvmDeclaration will be dropped in future versions of Kotlin. " +
            "See https://youtrack.jetbrains.com/issue/KT-67202"
)
@DeprecatedSinceKotlin(errorSince = "2.1")
public actual annotation class ImplicitlyActualizedByJvmDeclaration

/**
 * Instructs the Kotlin compiler not to generate getters/setters for this property and expose it as a field.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#instance-fields)
 * for more information.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public actual annotation class JvmField

/**
 * Instructs the compiler to generate or omit wildcards for type arguments corresponding to parameters with declaration-site variance,
 * for example such as `E` of `kotlin.collections.Collection` which is declared with an `out` variance.
 *
 * - If the innermost applied `@JvmSuppressWildcards` has `suppress=true` and the type is not annotated with `@JvmWildcard`, the type is
 * generated without wildcards.
 * - If the innermost applied `@JvmSuppressWildcards` has `suppress=false`, the type is generated with wildcards.
 *
 * It may be helpful only if declaration seems to be inconvenient to use from Java.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/java-to-kotlin-interop.html#variant-generics)
 * for more information.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public actual annotation class JvmSuppressWildcards(actual val suppress: Boolean = true)

/**
 * Instructs the compiler to generate wildcard for annotated type arguments corresponding to parameters with declaration-site variance.
 *
 * It may be helpful only if declaration seems to be inconvenient to use from Java without wildcard.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/java-to-kotlin-interop.html#variant-generics)
 * for more information.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public actual annotation class JvmWildcard

/**
 * Specifies that given value class is inline class.
 *
 * Adding or removing the annotation is a binary-incompatible change, since methods of inline classes
 * and functions with inline classes in their signatures are mangled.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@SinceKotlin("1.5")
public actual annotation class JvmInline

/**
 * Instructs compiler to mark the class as a record and generate relevant toString/equals/hashCode methods
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@SinceKotlin("1.5")
public actual annotation class JvmRecord

/**
 * This annotation instructs the compiler to expose the API of functions with inline classes
 * (and the classes containing them, including inline classes themselves)
 * as their boxed variant for effective usage from Java.
 *
 * It performs the following transformations:
 *
 * - For annotated functions and constructors that take or return inline classes,
 *   an unmangled wrapper function is created where inline classes are boxed.
 *   The wrapper is thus visible and callable from Java.
 *
 * - If class is annotated, the annotation implicitly propagates to its methods, forcing
 *   the compiler to generate wrappers for them.
 *
 * - A constructor is made available from Java.
 *
 * These changes maintain backward compatibility, allowing existing API to be safely marked.
 *
 * @property jvmName optional wrapper name. Only applicable to functions, getters and setters.
 */
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("2.2")
@ExperimentalStdlibApi
@Target(
    // function-like
    AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER,
    // containers
    AnnotationTarget.CLASS,
)
public actual annotation class JvmExposeBoxed(actual val jvmName: String)

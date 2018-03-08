/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public annotation class JvmOverloads

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
public annotation class JvmStatic

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
public annotation class JvmName(val name: String)

/**
 * Instructs the Kotlin compiler to generate a multifile class with top-level functions and properties declared in this file as one of its parts.
 * Name of the corresponding multifile class is provided by the [JvmName] annotation.
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public annotation class JvmMultifileClass

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
internal annotation class JvmPackageName(val name: String)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
public annotation class JvmSynthetic

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
 * Instructs the Kotlin compiler not to generate getters/setters for this property and expose it as a field.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#instance-fields)
 * for more information.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class JvmField

/**
 * Instructs compiler to generate or omit wildcards for type arguments corresponding to parameters with
 * declaration-site variance, for example such as `Collection<out T>` has.
 *
 * If the innermost applied `@JvmSuppressWildcards` has `suppress=true`, the type is generated without wildcards.
 * If the innermost applied `@JvmSuppressWildcards` has `suppress=false`, the type is generated with wildcards.
 *
 * It may be helpful only if declaration seems to be inconvenient to use from Java.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class JvmSuppressWildcards(val suppress: Boolean = true)

/**
 * Instructs compiler to generate wildcard for annotated type arguments corresponding to parameters with declaration-site variance.
 *
 * It may be helpful only if declaration seems to be inconvenient to use from Java without wildcard.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class JvmWildcard

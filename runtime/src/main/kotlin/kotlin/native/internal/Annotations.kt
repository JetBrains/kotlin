/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.native.internal

/**
 * Makes this function to be possible to call by given name from C++ part of runtime using C ABI.
 * The parameters are mapped in an implementation-dependent manner.
 *
 * The function to call from C++ can be a wrapper around the original function.
 *
 * If the name is not specified, the function to call will be available by its Kotlin unqualified name.
 *
 * This annotation is not intended for the general consumption and is public only for the launcher!
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
public annotation class ExportForCppRuntime(val name: String = "")

/**
 * This annotation denotes that the element is intrinsic and its usages require special handling in compiler.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class Intrinsic

/**
 * Exports symbol for compiler needs.
 *
 * This annotation is not intended for the general consumption and is public only for interop!
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class ExportForCompiler

/**
 * Annotated constructor will be inlined.
 */
@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
internal annotation class InlineConstructor

/**
 * Class is frozen by default. Also this annotation is (ab)used for marking objects
 * where mutability checks are not needed, and they are shared, such as atomics.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class Frozen

/**
 * Fields of annotated class won't be sorted.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class NoReorderFields

/**
 * Exports the TypeInfo of this class by given name to use it from runtime.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class ExportTypeInfo(val name: String)

/**
 * If a lambda shall be carefully lowered by the compiler.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class VolatileLambda

/**
 * Need to be fixed because of reflection.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
internal annotation class FixmeReflection

/**
 * Need to be fixed because of concurrency.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
internal annotation class FixmeConcurrency

/**
 * Escape analysis annotations.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class Escapes(val who: Int)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class PointsTo(vararg val onWhom: Int)

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

package kotlin.native

import kotlin.reflect.KClass

/**
 * Forces the compiler to use specified symbol name for the target `external` function.
 *
 * TODO: changing symbol name breaks the binary compatibility,
 * so it should probably be allowed on `internal` and `private` functions only.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class SymbolName(val name: String)

/**
 * Preserve the function entry point during global optimizations.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Retain

// TODO: merge with [kotlin.jvm.Throws]
/**
 * This annotation indicates what exceptions should be declared by a function when compiled to a platform method.
 *
 * When compiling to Objective-C/Swift framework, methods having or inheriting this annotation are represented as
 * `NSError*`-producing methods in Objective-C and as `throws` methods in Swift.
 * When such a method called through framework API throws an exception, it is either propagated as
 * `NSError` or considered unhandled (if exception `is` [kotlin.Error] or [kotlin.RuntimeException]).
 * In any case exception is not checked to be instance of one of the [exceptionClasses].
 *
 * @property exceptionClasses the list of checked exception classes that may be thrown by the function.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
public annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

/**
 * Top level variable or object is thread local, and so could be mutable.
 * One may use this annotation as the stopgap measure for singleton
 * object immutability.
 * PLEASE NOTE THAT THIS ANNOTATION MAY GO AWAY IN UPCOMING RELEASES.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class ThreadLocal

/**
 * Makes top level function available from C/C++ code with the given name.
 * `fullName` controls the name of top level function, `shortName` controls the short name.
 * If `fullName` is empty, no top level declaration is being created.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class CName(val fullName: String = "", val shortName: String = "")
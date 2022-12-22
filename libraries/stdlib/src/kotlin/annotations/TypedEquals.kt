/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Signals that annotated function is a typed equals declaration.
 *
 * ## Typed equals in value classes
 * Customization of equality relation for a class might require overriding `equals(other: Any?)` method. However, overriding only
 * `equals(other: Any?)` in a value class would require boxing of the right-hand side operand of every `==` comparison. For example:
 * ```
 * @JvmInline
 * value class Degrees(val value: Double) {
 *  override fun equals(other: Any?): Boolean {
 *      ...
 *  }
 * }
 *
 * fun foo() {
 *  // boxing when upcasting to Any?
 *  println(Degrees(0) == Degrees(45))
 * }
 * ```
 * Declaring typed equals function for value class is intended to avoid boxing. For example:
 * ```
 * @JvmInline
 * @AllowTypedEquals
 * value class Degrees(val value: Double) {
 *  @TypedEquals
 *  fun equals(other: Degrees) = (value - other.value) % 360.0 == 0.0
 * }
 * ```
 *
 * More precise, type equals function must be:
 * - Named `equals`
 * - Be declared as a member function of value class
 * - Have a single parameter which type is a star-projection of enclosing class
 * - Returns `Boolean`
 *
 * Typed equals, as well as equals from `Any`, must define an equivalence relation, i.e. be symmetric, reflexive, transitive and consistent.
 *
 * Typed equals function can not have type parameters.
 *
 * ## Interaction of typed equals and equals from `Any`
 * Typed equals and equals from `Any` should be consistent. More precise, For any `x`, `y` such that `x`, `y` refer instances of an inline
 * class, it must be true that `x.equals(y) == (x).equals(y as Any)`. There are three cases possible:
 * - Only typed equals is declared. The compiler will generate consistent equals from `Any`. Compiler-generated implementation will be
 *trying to cast the argument to corresponding value class type and passing to typed equals if succeeded, otherwise return `false`.
 * - Only equals from `Any` is declared. The compiler will generate consistent typed one. Compiler-generated typed equals will be boxing
 * argument and passing it to untyped equals. The compiler will warn user about negative performance impact of boxing in auto-generated
 * code.
 * - Both equals methods are declared. The compiler considers user responsible for their consistency.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public annotation class TypedEquals
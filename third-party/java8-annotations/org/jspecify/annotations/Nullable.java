/*
 * Copyright 2018-2020 The JSpecify Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jspecify.annotations;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated <a href="https://github.com/jspecify/jspecify/wiki/type-usages">type
 * usage</a> (commonly a parameter type or return type) is considered to include {@code null} as a
 * value.
 *
 * <p>Example usages:
 *
 * <pre>{@code
 * @Nullable String field;
 *
 * @Nullable String getField() { return field; }
 *
 * void setField(@Nullable String value) { field = value; }
 *
 * List<@Nullable String> getList() { … }
 * }</pre>
 *
 * <p>For a comprehensive introduction to JSpecify, please see <a
 * href="http://jspecify.org">jspecify.org</a>.
 *
 * <p><b>Warning:</b> These annotations are under development, and <b>any</b> aspect of their
 * naming, locations, or design is subject to change until the JSpecify 1.0 release. Moreover,
 * supporting analysis tools will be tracking the changes on varying schedules. Releasing a library
 * using these annotations in its API is <b>strongly discouraged</b> at this time.
 *
 * <h2>Meaning per each kind of type usage</h2>
 *
 * <p>The essential meaning of this annotation is always the same: the type it annotates is
 * considered to include {@code null} as a value. But this may affect your code a little differently
 * based on the kind of type usage involved.
 *
 * <ul>
 *   <li>On a <b>parameter type</b>: The {@code setField} method (at top) permissively accepts a
 *       "string-or-null", meaning that it is okay to pass an actual string, or to pass {@code
 *       null}. (This doesn't guarantee that passing {@code null} won't produce an exception at
 *       runtime, but it should be much less likely.) This also applies to the type of a lambda
 *       expression parameter, if that type is given explicitly (otherwise its nullness must be
 *       inferred from context).
 *   <li>On a <b>method return type</b>: The {@code getField} method returns a "string-or-null", so
 *       while the caller might get a string back, it should also address the possibility of getting
 *       {@code null} instead. (This doesn't guarantee there is any circumstance in which {@code
 *       null} <i>will</i> actually be returned.)
 *   <li>On a <b>field type</b>: The {@code field} field has the type "string-or-null", so at times
 *       it might hold a string, and at times it might hold {@code null}. (Of course, every field of
 *       a reference type <i>originally</i> holds {@code null}, but as long as the class ensures
 *       that its uninitialized states can't be observed, it's appropriate to overlook that fact.)
 *   <li>On a <b>type argument</b>: A type usage of "nullable string" appears <i>within</i> the
 *       compound type {@code List<@Nullable String>}. No matter how this type is used (return type,
 *       etc.), this means the same thing: every appearance of {@code E} in {@code List}'s member
 *       signatures will be considered nullable. For a list, this means it may contain null
 *       <i>elements</i>. If the list reference itself might be null as well, we can write
 *       {@code @Nullable List<@Nullable String>}, a "nullable list of nullable strings".
 *   <li>On the upper bound of a <b>type parameter</b>: For example, as seen in {@code class List<E
 *       extends @Nullable Object>}. This means that a <i>type argument</i> supplied for that type
 *       parameter is permitted to be nullable if desired: {@code List<@Nullable String>}. (A
 *       non-null type argument, as in {@code List<String>}, is permitted either way.)
 *   <li>On a usage of a <b>type variable</b>: A type parameter, like the {@code E} in {@code
 *       interface List<E>}, defines a "type variable" of the same name, usable only <i>within</i>
 *       the scope of the declaring API element. In any example using {@code String} above, a type
 *       variable like {@code E} might appear instead. {@code @Nullable} continues to mean "or null"
 *       as always, but notably, this works without regard to whether the type argument is
 *       <i>already</i> nullable. For example, suppose that {@code class Foo<E extends @Nullable
 *       Object>} has a method {@code @Nullable E eOrNull()}. Then, whether {@code foo} is of type
 *       {@code Foo<String>} or {@code Foo<@Nullable String>}, the expression {@code foo.eOrNull()}
 *       is nullable either way. Using {@code @Nullable E} in this way is called "nullable
 *       projection" (<a href="NonNull.html#projection">non-null projection</a> is likewise
 *       supported, but less commonly useful).
 *   <li>On a <b>nested type</b>: In most examples above, in place of {@code String} we might use a
 *       nested type such as {@code Map.Entry}. The Java syntax for annotating such a type as
 *       nullable looks like {@code Map.@Nullable Entry}.
 *   <li>On a <b>record component</b>: As expected, {@code @Nullable} here applies equally to the
 *       corresponding parameter type of the canonical constructor, and to the return type of a
 *       generated accessor method as well. If an explicit accessor method is provided for this
 *       record component, it must still be annotated explicitly. Any non-null components should be
 *       checked (for example using {@link java.util.Objects#requireNonNull}) in a <a
 *       href="https://docs.oracle.com/en/java/javase/19/language/records.html">compact
 *       constructor</a>.
 * </ul>
 *
 * <h2 id="applicability">Where it is applicable</h2>
 *
 * <p>This annotation and {@link NonNull} are applicable to any <a
 * href="https://github.com/jspecify/jspecify/wiki/type-usages">type usage</a> <b>except</b> the
 * following cases, where they have no defined meaning:
 *
 * <ul>
 *   <li>On any<b> intrinsically non-null type usage</b>. Some type usages are incapable of
 *       including {@code null} by the rules of the Java language. Examples include any usage of a
 *       primitive type, the argument to {@code instanceof}, a method return type in an annotation
 *       interface, or the type following {@code throws} or {@code catch}. In such locations, a
 *       nullness annotation could only be contradictory ({@code @Nullable}) or redundant
 *       ({@code @NonNull}).
 *   <li>On the root type of a <b>local variable</b> declaration. The nullness of a local variable
 *       itself is not a fixed declarative property of its <i>type</i>. Rather it should be inferred
 *       from the nullness of each expression assigned to the variable, possibly changing over time.
 *       (<a href="https://bit.ly/3ppb8ZC">Why?</a>). Subcomponents of the type (type arguments,
 *       array component types) are annotatable as usual.
 *   <li>On the root type in a <b>cast expression</b>. To inform an analyzer that an expression it
 *       sees as nullable is truly non-null, use an assertion or a method like {@link
 *       java.util.Objects#requireNonNull}. (<a href="https://bit.ly/3ppb8ZC">Why?</a>)
 *       Subcomponents of the type (type arguments, array component types) are annotatable as usual.
 *   <li>On any part of a <b>receiver parameter</b> type (<a
 *       href="https://docs.oracle.com/javase/specs/jls/se18/html/jls-8.html#jls-8.4">JLS 8.4</a>).
 *   <li>If both {@code @Nullable} and {@code @NonNull} appear on the same type usage,
 *       <i>neither</i> one is recognized.
 * </ul>
 *
 * Whether the code is {@link NullMarked} also has no consequence in the above locations.
 *
 * <h2>Unannotated type usages</h2>
 *
 * <p>For a type usage where nullness annotations are <a href="#applicability">applicable</a> but
 * not present, its nullness depends on whether it appears within {@linkplain NullMarked
 * null-marked} code; see that class for details. Note in particular that nullness information from
 * a superclass is never automatically "inherited".
 */
@Documented
@Target(TYPE_USE)
@Retention(RUNTIME)
public @interface Nullable {}

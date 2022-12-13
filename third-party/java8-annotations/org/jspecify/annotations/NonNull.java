/*
 * Copyright 2022 The JSpecify Authors.
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
 * usage</a> (commonly a parameter type or return type) is considered to <i>exclude</i> {@code null}
 * as a value; rarely needed within {@linkplain NullMarked null-marked} code.
 *
 * <p>This annotation serves two primary purposes:
 *
 * <ul>
 *   <li>To mark any sporadic non-null type usages inside a scope that is not ready to be fully
 *       {@link NullMarked} yet.
 *   <li>To perform a <i>non-null projection</i> of a type variable, explained below.
 * </ul>
 *
 * <p>For a comprehensive introduction to JSpecify, please see <a
 * href="http://jspecify.org">jspecify.org</a>.
 *
 * <p><b>Warning:</b> These annotations are under development, and <b>any</b> aspect of their
 * naming, locations, or design is subject to change until the JSpecify 1.0 release. Moreover,
 * supporting analysis tools will be tracking the changes on varying schedules. Releasing a library
 * using these annotations in its API is <b>strongly discouraged</b> at this time.
 *
 * <h2 id="projection">Non-null projection</h2>
 *
 * <p>In the following example, {@code MyOptional}'s type parameter {@code T} accepts only non-null
 * type arguments, but {@code MyList}'s type parameter {@code E} will accept either a non-null or
 * nullable type argument.
 *
 * <pre>{@code
 * // All the below is null-marked code
 *
 * class MyOptional<T> { … }
 *
 * interface MyList<E extends @Nullable Object> {
 *   // Returns the first non-null element, if such element exists.
 *   MyOptional<E> firstNonNull() { … } // problem here!
 * }
 *
 * MyList<@Nullable String> maybeNulls = …
 * MyList<String> nonNulls = …
 * }</pre>
 *
 * <p>Because {@code MyOptional} accepts only non-null type arguments, we need both {@code
 * maybeNulls.firstNonNull()} and {@code nonNulls.firstNonNull()} to produce the same return type:
 * {@code MyOptional!<String!>} (see <a
 * href="https://github.com/jspecify/jspecify/wiki/notation#shorthand-notation">notation</a>).
 * However, as specified above, they won't do that. In fact, there is a problem with the {@code
 * firstNonNull} signature, since the type argument {@code String?} would not meet the requirements
 * of {@code MyOptional}'s type parameter.
 *
 * <p>The solution is to <b>project</b> the type argument to its non-null counterpart:
 *
 * <pre>{@code
 * // Returns the first non-null element, if such element exists.
 * MyOptional<@NonNull E> firstNonNull() { … } // problem fixed!
 * }</pre>
 *
 * <p>Here, {@code @NonNull E} selects the non-null form of the type argument, whether it was
 * already non-null or not, which is just what we need in this scenario.
 *
 * <p>If {@code E} has a non-null upper bound, then the apparent projection {@code @NonNull E} is
 * redundant but harmless.
 *
 * <p><a href="Nullable.html#projection">Nullable projection</a> serves the equivalent purpose in
 * the opposite direction, and is far more commonly useful.
 *
 * <p>If a type variable has <i>all</i> its usages being projected in one direction or the other, it
 * should be given a non-null upper bound, and any non-null projections can then be removed.
 *
 * <h2>Where it is not applicable</h2>
 *
 * <p>{@code @NonNull} is inapplicable in all the <a href="Nullable.html#applicability">same
 * locations</a> as {@link Nullable}.
 */
@Documented
@Target(TYPE_USE)
@Retention(RUNTIME)
public @interface NonNull {}

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

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element and the code transitively {@linkplain
 * javax.lang.model.element.Element#getEnclosedElements() enclosed} within it are <b>null-marked
 * code</b>: there, type usages are generally considered to exclude {@code null} as a value unless
 * specified otherwise. Using this annotation avoids the need to write {@link NonNull @NonNull} many
 * times throughout your code.
 *
 * <p>For a comprehensive introduction to JSpecify, please see <a
 * href="http://jspecify.org">jspecify.org</a>.
 *
 * <p><b>Warning:</b> These annotations are under development, and <b>any</b> aspect of their
 * naming, locations, or design is subject to change until the JSpecify 1.0 release. Moreover,
 * supporting analysis tools will be tracking the changes on varying schedules. Releasing a library
 * using these annotations in its API is <b>strongly discouraged</b> at this time.
 *
 * <h2 id="effects">Effects of being null-marked</h2>
 *
 * <p>Within null-marked code, as a <i>general</i> rule, a type usage is considered non-null (to
 * exclude {@code null} as a value) unless explicitly annotated as {@link Nullable}. However, there
 * are several special cases to address.
 *
 * <h3 id="effects-special-cases">Special cases</h3>
 *
 * <p>Within null-marked code:
 *
 * <ul>
 *   <li>We might expect the type represented by a <b>wildcard</b> (like the {@code ?} in {@code
 *       List<?>}) to be non-null, but it isn't necessarily. It's non-null only if it {@code
 *       extends} a non-null type (like in {@code List<? extends String>}), or if the <i>class</i>
 *       in use accepts only non-null type arguments (such as if {@code List} were declared as
 *       {@code class List<E extends String>}). But if {@code List} does accept nullable type
 *       arguments, then the wildcards seen in {@code List<?>} and {@code List<? super String>} must
 *       include {@code null}, because they have no "upper bound". (<a
 *       href="https://bit.ly/3ppb8ZC">Why?</a>)
 *       <ul>
 *         <li>Conversely, a <b>type parameter</b> is always considered to have an upper bound; when
 *             none is given explicitly, {@code Object} is filled in by the compiler. The example
 *             {@code class MyList<E>} is interpreted identically to {@code class MyList<E extends
 *             Object>}: in both cases the type argument in {@code MyList<@Nullable Foo>} is
 *             out-of-bounds, so the list elements are always non-null. (<a
 *             href="https://bit.ly/3ppb8ZC">Why?</a>)
 *       </ul>
 *   <li>Otherwise, being null-marked has no consequence for any type usage where {@code @Nullable}
 *       and {@code @NonNull} are <a href="Nullable.html#applicability"><b>not applicable</b></a>,
 *       such as the root type of a local variable declaration.
 *   <li>When a type variable has a nullable upper bound, such as the {@code E} in {@code class
 *       Foo<E extends @Nullable Bar>}), an unannotated usage of this type variable is not
 *       considered nullable, non-null, or even of "unspecified" nullness. Rather it has
 *       <b>parametric nullness</b>. In order to support both nullable and non-null type arguments
 *       safely, the {@code E} type itself must be handled <i>strictly</i>: as if nullable when
 *       "read from", but as if non-null when "written to". (Contrast with {@code class Foo<E
 *       extends Bar>}, where usages of {@code E} are simply non-null, just like usages of {@code
 *       String} are.)
 *   <li>By using {@link NullUnmarked}, an element within null-marked code can be excluded and made
 *       null-unmarked, exactly as if there were no enclosing {@code @NullMarked} element at all.
 * </ul>
 *
 * <h2 id="where">Where it can be used</h2>
 *
 * {@code @NullMarked} and {@link NullUnmarked @NullUnmarked} can be used on any package, class,
 * method, or constructor declaration; {@code @NullMarked} can be used on a module declaration as
 * well. Special considerations:
 *
 * <ul>
 *   <li>To apply this annotation to an entire (single) <b>package</b>, create a <a
 *       href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-7.html#jls-7.4.1">{@code
 *       package-info.java}</a> file there. This is recommended so that newly-created classes will
 *       be null-marked by default. This annotation has no effect on "subpackages". <b>Warning</b>:
 *       if the package does not belong to a module, be very careful: it can easily happen that
 *       different versions of the package-info file are seen and used in different circumstances,
 *       causing the same classes to be interpreted inconsistently. For example, a package-info file
 *       from a {@code test} source path might hide the corresponding one from the {@code main}
 *       source path, or generated code might be compiled without seeing a package-info file at all.
 *   <li>Although Java permits it to be applied to a <b>record component</b> declaration (as in
 *       {@code record Foo(@NullMarked String bar) {...}}), this annotation has no meaning when used
 *       in that way.
 *   <li>Applying this annotation to an instance <b>method</b> of a <i>generic</i> class is
 *       acceptable, but is not recommended because it can lead to some confusing situations.
 *   <li>An advantage of Java <b>modules</b> is that you can make a lot of code null-marked with
 *       just a single annotation (before the {@code module} keyword). {@link NullUnmarked} is not
 *       supported on modules, since it's already the default.
 *   <li>If both {@code @NullMarked} and {@code @NullUnmarked} appear together on the same element,
 *       <i>neither</i> one is recognized.
 * </ul>
 */
@Documented
@Target({PACKAGE, TYPE, METHOD, CONSTRUCTOR})
@Retention(RUNTIME)
public @interface NullMarked {}

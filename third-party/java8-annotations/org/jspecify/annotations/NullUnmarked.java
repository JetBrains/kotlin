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

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element and the code transitively {@linkplain
 * javax.lang.model.element.Element#getEnclosedElements() enclosed} within it is <b>null-unmarked
 * code</b>: there, type usages generally have <b>unspecified nullness</b> unless explicitly
 * annotated otherwise.
 *
 * <p>This annotation's purpose is to ease migration of a large existing codebase to null-marked
 * status. It makes it possible to "flip the default" for new code added to a class or package even
 * before that class or package has been fully migrated. Since new code is the most important code
 * to analyze, this is strongly recommended as a temporary measure whenever necessary. However, once
 * a codebase has been fully migrated it would be appropriate to ban use of this annotation.
 *
 * <p>For a comprehensive introduction to JSpecify, please see <a
 * href="http://jspecify.org">jspecify.org</a>.
 *
 * <p><b>Warning:</b> These annotations are under development, and <b>any</b> aspect of their
 * naming, locations, or design is subject to change until the JSpecify 1.0 release. Moreover,
 * supporting analysis tools will track with these changes on varying schedules. Releasing a library
 * using these annotations in its API is <b>strongly discouraged</b> at this time.
 *
 * <h2>Null-marked and null-unmarked code</h2>
 *
 * <p>{@link NullMarked} and this annotation work as a pair to include and exclude sections of code
 * from null-marked status (respectively). Specifically, code is considered null-marked if the most
 * narrowly enclosing element annotated with either of these two annotations exists and is annotated
 * with {@code @NullMarked}.
 *
 * <p>Otherwise it is considered null-unmarked. This can happen in two ways: either it is more
 * narrowly enclosed by a {@code @NullUnmarked}-annotated element than by any
 * {@code @NullMarked}-annotated element, or neither annotation is present on any enclosing element.
 * No distinction is made between these cases.
 *
 * <p>The effects of being null-marked are described in the <a
 * href="NullMarked.html#effects">Effects</a> section of {@code NullMarked}.
 *
 * <h2>Unspecified nullness</h2>
 *
 * <p>Within null-unmarked code, a type usage with no nullness annotation has <b>unspecified
 * nullness</b> (<a href="https://bit.ly/3ppb8ZC">Why?</a>). This means that, while there is always
 * <i>some</i> correct way to annotate it for nullness, that information is missing: we <i>do not
 * know</i> whether it includes or excludes {@code null} as a value. In such a case, tools can vary
 * widely in how strict or lenient their enforcement is, or might make it configurable.
 *
 * <p>For more, please see this more <a
 * href="https://github.com/jspecify/jspecify/wiki/nullness-unspecified">comprehensive
 * discussion</a> of unspecified nullness.
 *
 * <p>There is no way for an individual type usage within null-marked code to have unspecified
 * nullness. (<a href="https://bit.ly/3ppb8ZC">Why?</a>)
 *
 * <h2>Where it can be used</h2>
 *
 * The information in the <a href="NullMarked.html#where">Where it can be used</a> section of {@code
 * NullMarked} applies as well to this annotation.
 */
// TODO(kevinb9n): word the middle section better with good words
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({PACKAGE, TYPE, METHOD, CONSTRUCTOR})
public @interface NullUnmarked {}

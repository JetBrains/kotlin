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
package org.jspecify.nullness;

import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <b>WARNING:</b> This is not the final class name or package name for this annotation. In
 * addition, we are still discussing questions about semantics, particularly around type-variable
 * usages. After that, changes and documentation will follow.
 *
 * <p>These annotations exist only as a skeleton for the final product. At this point, we are not
 * even building prototypes that use them.
 */
@Documented
@Target({TYPE, PACKAGE, MODULE})
@Retention(RUNTIME)
public @interface NullMarked {}
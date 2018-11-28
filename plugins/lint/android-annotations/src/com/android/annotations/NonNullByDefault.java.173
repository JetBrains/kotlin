/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Denotes that all parameters, fields or methods within a class or method by
 * default can not be null. This can be overridden by adding specific
 * {@link com.android.annotations.Nullable} annotations on fields, parameters or
 * methods that should not use the default.
 * <p/>
 * NOTE: Eclipse does not yet handle defaults well (in particular, if
 * you add this on a class which implements Comparable, then it will insist
 * that your compare method is changing the nullness of the compare parameter,
 * so you'll need to add @Nullable on it, which also is not right (since
 * the method should have implied @NonNull and you do not need to check
 * the parameter.). For now, it's best to individually annotate methods,
 * parameters and fields.
 * <p/>
 * This is a marker annotation and it has no specific attributes.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({PACKAGE, TYPE})
public @interface NonNullByDefault {
}

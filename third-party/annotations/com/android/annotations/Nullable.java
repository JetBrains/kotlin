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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that a parameter, field or method return value can be null.
 * <b>Note</b>: this is the default assumption for most Java APIs and the
 * default assumption made by most static code checking tools, so usually you
 * don't need to use this annotation; its primary use is to override a default
 * wider annotation like {@link NonNullByDefault}.
 * <p/>
 * When decorating a method call parameter, this denotes the parameter can
 * legitimately be null and the method will gracefully deal with it. Typically
 * used on optional parameters.
 * <p/>
 * When decorating a method, this denotes the method might legitimately return
 * null.
 * <p/>
 * This is a marker annotation and it has no specific attributes.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({METHOD, PARAMETER, LOCAL_VARIABLE, FIELD})
public @interface Nullable {
}

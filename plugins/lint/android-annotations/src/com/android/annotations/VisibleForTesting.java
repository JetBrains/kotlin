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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Denotes that the class, method or field has its visibility relaxed so
 * that unit tests can access it.
 * <p/>
 * The <code>visibility</code> argument can be used to specific what the original
 * visibility should have been if it had not been made public or package-private for testing.
 * The default is to consider the element private.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface VisibleForTesting {
    /**
     * Intended visibility if the element had not been made public or package-private for
     * testing.
     */
    enum Visibility {
        /** The element should be considered protected. */
        PROTECTED,
        /** The element should be considered package-private. */
        PACKAGE,
        /** The element should be considered private. */
        PRIVATE
    }

    /**
     * Intended visibility if the element had not been made public or package-private for testing.
     * If not specified, one should assume the element originally intended to be private.
     */
    Visibility visibility() default Visibility.PRIVATE;
}

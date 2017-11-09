/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.tools.klint.checks;

import com.android.annotations.NonNull;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Represents a package and its classes
 */
public class ApiPackage implements Comparable<ApiPackage> {
    private final String mName;
    private final List<ApiClass> mClasses = Lists.newArrayListWithExpectedSize(100);

    // Persistence data: Used when writing out binary data in ApiLookup
    int indexOffset;         // offset of the package entry

    ApiPackage(@NonNull String name) {
        mName = name;
    }

    /**
     * Returns the name of the class (fully qualified name)
     * @return the name of the class
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Returns the classes in this package
     * @return the classes in this package
     */
    @NonNull
    public List<ApiClass> getClasses() {
        return mClasses;
    }

    void addClass(@NonNull ApiClass clz) {
        mClasses.add(clz);
    }

    @Override
    public int compareTo(@NonNull ApiPackage other) {
        return mName.compareTo(other.mName);
    }

    @Override
    public String toString() {
        return mName;
    }
}

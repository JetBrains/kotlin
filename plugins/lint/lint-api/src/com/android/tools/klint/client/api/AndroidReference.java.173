/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.tools.klint.client.api;

import static com.android.SdkConstants.ANDROID_PKG;

import com.android.annotations.NonNull;
import com.android.resources.ResourceType;

import org.jetbrains.uast.UExpression;

public class AndroidReference {
    public final UExpression node;

    private final String rPackage;

    private final ResourceType type;

    private final String name;

    // getPackage() can be empty if not a package-qualified import (e.g. android.R.id.name).
    @NonNull
    public String getPackage() {
        return rPackage;
    }

    @NonNull
    public ResourceType getType() {
        return type;
    }

    @NonNull
    public String getName() {
        return name;
    }

    boolean isFramework() {
        return rPackage.equals(ANDROID_PKG);
    }

    public AndroidReference(
            UExpression node,
            String rPackage,
            ResourceType type,
            String name) {
        this.node = node;
        this.rPackage = rPackage;
        this.type = type;
        this.name = name;
    }
}

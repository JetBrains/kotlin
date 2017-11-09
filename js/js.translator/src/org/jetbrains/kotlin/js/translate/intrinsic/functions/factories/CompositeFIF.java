/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic;

import java.util.List;
import java.util.function.Predicate;

public abstract class CompositeFIF implements FunctionIntrinsicFactory {
    @NotNull
    private final List<Pair<Predicate<FunctionDescriptor>, FunctionIntrinsic>> patternsAndIntrinsics = Lists.newArrayList();

    protected CompositeFIF() {
    }

    @Nullable
    @Override
    public FunctionIntrinsic getIntrinsic(@NotNull FunctionDescriptor descriptor) {
        for (Pair<Predicate<FunctionDescriptor>, FunctionIntrinsic> entry : patternsAndIntrinsics) {
            if (entry.first.test(descriptor)) {
                return entry.second;
            }
        }
        return null;
    }

    protected void add(@NotNull Predicate<FunctionDescriptor> pattern, @NotNull FunctionIntrinsic intrinsic) {
        patternsAndIntrinsics.add(Pair.create(pattern, intrinsic));
    }
}

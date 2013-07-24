/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.intrinsic.functions.factories;

import com.google.common.base.Predicate;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;

import java.util.ArrayList;
import java.util.List;

public abstract class CompositeFIF implements FunctionIntrinsicFactory {
    @NotNull
    private final List<Pair<DescriptorPredicate, FunctionIntrinsic>> patternsAndIntrinsics = new ArrayList<Pair<DescriptorPredicate, FunctionIntrinsic>>();

    protected CompositeFIF() {
    }

    @NotNull
    @Override
    public Predicate<FunctionDescriptor> getPredicate() {
        return new DescriptorPredicate() {
            @Override
            public boolean apply(@Nullable FunctionDescriptor descriptor) {
                return descriptor != null && findIntrinsic(descriptor) != null;
            }
        };
    }

    @Nullable
    public FunctionIntrinsic findIntrinsic(@NotNull FunctionDescriptor descriptor) {
        for (Pair<DescriptorPredicate, FunctionIntrinsic> entry : patternsAndIntrinsics) {
            if (entry.first.apply(descriptor)) {
                return entry.second;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public FunctionIntrinsic getIntrinsic(@NotNull FunctionDescriptor descriptor) {
        FunctionIntrinsic intrinsic = findIntrinsic(descriptor);
        assert intrinsic != null;
        return intrinsic;
    }

    protected void add(@NotNull DescriptorPredicate pattern, @NotNull FunctionIntrinsic intrinsic) {
        patternsAndIntrinsics.add(Pair.create(pattern, intrinsic));
    }
}

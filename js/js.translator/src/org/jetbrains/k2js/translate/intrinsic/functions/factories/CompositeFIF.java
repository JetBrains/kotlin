/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;

import java.util.Collection;
import java.util.Map;

public abstract class CompositeFIF implements FunctionIntrinsicFactory {

    @NotNull final Map<DescriptorPredicate, FunctionIntrinsic> patternToIntrinsic = Maps.newHashMap();

    protected CompositeFIF() {
    }

    @NotNull
    @Override
    public Predicate<FunctionDescriptor> getPredicate() {
        Collection<DescriptorPredicate> patterns = patternToIntrinsic.keySet();
        final DescriptorPredicate[] patterns1 = patterns.toArray(new DescriptorPredicate[patterns.size()]);
        return new DescriptorPredicate() {
            @Override
            public boolean apply(@Nullable FunctionDescriptor descriptor) {
                return Predicates.or(patterns1).apply(descriptor);
            }
        };
    }

    @NotNull
    @Override
    public FunctionIntrinsic getIntrinsic(@NotNull FunctionDescriptor descriptor) {
        for (DescriptorPredicate pattern : patternToIntrinsic.keySet()) {
            if (pattern.apply(descriptor)) {
                return patternToIntrinsic.get(pattern);
            }
        }
        throw new IllegalStateException("Must have intrinsic for pattern.");
    }

    protected void add(@NotNull DescriptorPredicate pattern, @NotNull FunctionIntrinsic intrinsic) {
        patternToIntrinsic.put(pattern, intrinsic);
    }
}

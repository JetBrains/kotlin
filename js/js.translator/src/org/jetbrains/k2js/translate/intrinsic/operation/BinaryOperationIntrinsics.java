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

package org.jetbrains.k2js.translate.intrinsic.operation;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

public final class BinaryOperationIntrinsics {

    @NotNull
    private final List<BinaryOperationIntrinsic> intrinsics = Lists.newArrayList();

    {
        intrinsics.add(new EqualsIntrinsic());
        intrinsics.add(new CompareToInstrinsic());
    }

    public BinaryOperationIntrinsics() {
    }

    @Nullable
    public BinaryOperationIntrinsic getIntrinsic(@NotNull JetBinaryExpression expression, @NotNull TranslationContext context) {
        for (BinaryOperationIntrinsic intrinsic : intrinsics) {
            if (intrinsic.isApplicable(expression, context)) {
                return intrinsic;
            }
        }
        return null;
    }
}

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
import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.BuiltInPropertyIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.List;

public abstract class CompositeFIF implements FunctionIntrinsicFactory {
    @NotNull
    public static final FunctionIntrinsic LENGTH_PROPERTY_INTRINSIC = new BuiltInPropertyIntrinsic("length");
    public static final FunctionIntrinsic IS_EMPTY_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver, @NotNull List<JsExpression> arguments, @NotNull TranslationContext context
        ) {
            assert receiver != null;
            return JsAstUtils.equality(new JsNameRef("length", receiver), context.program().getNumberLiteral(0));
        }
    };

    @NotNull
    private final List<Pair<Predicate<FunctionDescriptor>, FunctionIntrinsic>> patternsAndIntrinsics = Lists.newArrayList();

    protected CompositeFIF() {
    }

    @NotNull
    @Override
    public DescriptorPredicate getPredicate() {
        return new DescriptorPredicate() {
            @Override
            public boolean apply(@NotNull FunctionDescriptor descriptor) {
                return findIntrinsic(descriptor) != null;
            }
        };
    }

    @Nullable
    public FunctionIntrinsic findIntrinsic(@NotNull FunctionDescriptor descriptor) {
        for (Pair<Predicate<FunctionDescriptor>, FunctionIntrinsic> entry : patternsAndIntrinsics) {
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

    protected void add(@NotNull Predicate<FunctionDescriptor> pattern, @NotNull FunctionIntrinsic intrinsic) {
        patternsAndIntrinsics.add(Pair.create(pattern, intrinsic));
    }
 }

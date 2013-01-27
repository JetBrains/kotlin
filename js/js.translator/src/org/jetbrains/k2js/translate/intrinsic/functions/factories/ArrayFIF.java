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
import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.BuiltInFunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.assignment;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.inequality;

public final class ArrayFIF extends CompositeFIF {

    @NotNull
    private static final NamePredicate ARRAYS;

    static {
        List<Name> arrayTypeNames = Lists.newArrayList();
        for (PrimitiveType type : PrimitiveType.values()) {
            arrayTypeNames.add(type.getArrayTypeName());
        }
        arrayTypeNames.add(Name.identifier("Array"));
        ARRAYS = new NamePredicate(arrayTypeNames);
    }

    private static final FunctionIntrinsic ARRAY_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert arguments.size() == 1;
            return arguments.get(0);
        }
    };

    @NotNull
    public static final FunctionIntrinsic GET_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.size() == 1 : "Array get expression must have one argument.";
            JsExpression indexExpression = arguments.get(0);
            return new JsArrayAccess(receiver, indexExpression);
        }
    };

    @NotNull
    public static final FunctionIntrinsic SET_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            assert receiver != null;
            assert arguments.size() == 2 : "Array set expression must have two arguments.";
            JsExpression indexExpression = arguments.get(0);
            JsExpression value = arguments.get(1);
            JsArrayAccess arrayAccess = new JsArrayAccess(receiver, indexExpression);
            return assignment(arrayAccess, value);
        }
    };

    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new ArrayFIF();

    private ArrayFIF() {
        add(pattern(ARRAYS, "get"), GET_INTRINSIC);
        add(pattern(ARRAYS, "set"), SET_INTRINSIC);
        add(pattern(ARRAYS, "<get-size>"), LENGTH_PROPERTY_INTRINSIC);
        add(pattern(ARRAYS, "<get-indices>"), new KotlinFunctionIntrinsic("arrayIndices"));
        FunctionIntrinsic iteratorIntrinsic = new KotlinFunctionIntrinsic("arrayIterator");
        add(pattern(ARRAYS, "iterator"), iteratorIntrinsic);
        add(pattern(ARRAYS, "<init>"), new KotlinFunctionIntrinsic("arrayFromFun"));
        add(pattern("kotlin", "array"), ARRAY_INTRINSIC);
        add(new DescriptorPredicate() {
            @Override
            public boolean apply(@NotNull FunctionDescriptor descriptor) {
                for (PrimitiveType type : PrimitiveType.values()) {
                    if (type.getArrayTypeName().equals(descriptor.getName())) {
                        DeclarationDescriptor nsDeclaration = descriptor.getContainingDeclaration();
                        return nsDeclaration instanceof NamespaceDescriptor && DescriptorUtils.isRootNamespace((NamespaceDescriptor) nsDeclaration) && nsDeclaration.getName().getName().equals("kotlin");
                    }
                }
                return false;
            }
        }, ARRAY_INTRINSIC);

        add(pattern("java", "util", "ArrayList", "<init>"), new FunctionIntrinsic() {
            @NotNull
            @Override
            public JsExpression apply(
                    @Nullable JsExpression receiver,
                    @NotNull List<JsExpression> arguments,
                    @NotNull TranslationContext context
            ) {
                assert arguments.isEmpty();
                return new JsArrayLiteral();
            }
        });

        String[] list = {"jet", "List"};
        add(pattern(list, "size").checkOverridden(), LENGTH_PROPERTY_INTRINSIC);
        add(pattern(list, "get").checkOverridden(), new KotlinFunctionIntrinsic("arrayGet"));
        add(pattern(list, "isEmpty").checkOverridden(), IS_EMPTY_INTRINSIC);
        add(pattern(list, "iterator").checkOverridden(), iteratorIntrinsic);
        add(pattern(list, "indexOf").checkOverridden(), new KotlinFunctionIntrinsic("arrayIndexOf"));
        add(pattern(list, "lastIndexOf").checkOverridden(), new KotlinFunctionIntrinsic("arrayLastIndexOf"));
        add(pattern(list, "toArray").checkOverridden(), new FunctionIntrinsic() {
            @NotNull
            @Override
            public JsExpression apply(
                    @Nullable JsExpression receiver, @NotNull List<JsExpression> arguments, @NotNull TranslationContext context
            ) {
                assert receiver != null;
                return new JsInvocation(new JsNameRef("slice", receiver), context.program().getNumberLiteral(0));
            }
        });
        add(pattern(list, "contains").checkOverridden(), new FunctionIntrinsic() {
            @NotNull
            @Override
            public JsExpression apply(
                    @Nullable JsExpression receiver, @NotNull List<JsExpression> arguments, @NotNull TranslationContext context
            ) {
                assert receiver != null && arguments.size() == 1;
                return inequality(new JsInvocation(context.namer().kotlin("arrayIndexOf"), receiver, arguments.get(0)),
                                  context.program().getNumberLiteral(-1));
            }
        });

        add(pattern(list, "equals").checkOverridden(), new KotlinFunctionIntrinsic("arrayEquals"));
        add(pattern(list, "toString").checkOverridden(), new KotlinFunctionIntrinsic("arrayToString"));

        String[] mutableList = {"jet", "MutableList"};
        add(pattern(mutableList, "set").checkOverridden(), new KotlinFunctionIntrinsic("arraySet"));
        // http://jsperf.com/push-vs-len
        PatternBuilder.DescriptorPredicateImpl addPattern = pattern(mutableList, "add").checkOverridden();
        add(new ValueParametersAwareDescriptorPredicate(addPattern, 1), new BuiltInFunctionIntrinsic("push"));
        add(new ValueParametersAwareDescriptorPredicate(addPattern, 2), new KotlinFunctionIntrinsic("arrayAddAt"));
        add(pattern(mutableList, "addAll").checkOverridden(), new KotlinFunctionIntrinsic("arrayAddAll"));
        PatternBuilder.DescriptorPredicateImpl removePattern = pattern(mutableList, "remove").checkOverridden();
        add(new ValueParametersAwareDescriptorPredicate(removePattern, Predicates.equalTo(KotlinBuiltIns.getInstance().getIntType())),
            new KotlinFunctionIntrinsic("arrayRemoveAt"));
        add(new ValueParametersAwareDescriptorPredicate(removePattern, Predicates.equalTo(KotlinBuiltIns.getInstance().getNullableAnyType())),
            new KotlinFunctionIntrinsic("arrayRemove"));
        add(pattern(mutableList, "clear").checkOverridden(), new FunctionIntrinsic() {
            @NotNull
            @Override
            public JsExpression apply(
                    @Nullable JsExpression receiver, @NotNull List<JsExpression> arguments, @NotNull TranslationContext context
            ) {
                assert receiver != null;
                return assignment(new JsNameRef("length", receiver), context.program().getNumberLiteral(0));
            }
        });

        add(pattern("jet", "Iterable", "iterator").checkOverridden(), new KotlinFunctionIntrinsic("collectionIterator"));

        String[] collection = {"jet", "Collection"};
        add(pattern(collection, "size").checkOverridden(), new KotlinFunctionIntrinsic("collectionSize"));
        add(pattern(collection, "isEmpty").checkOverridden(), new KotlinFunctionIntrinsic("collectionIsEmpty"));
        add(pattern(collection, "contains").checkOverridden(), new KotlinFunctionIntrinsic("collectionContains"));

        String[] mutableCollection = {"jet", "MutableCollection"};
        add(pattern(mutableCollection, "add").checkOverridden(), new KotlinFunctionIntrinsic("collectionAdd"));
        add(pattern(mutableCollection, "addAll").checkOverridden(), new KotlinFunctionIntrinsic("collectionAddAll"));
        add(pattern(mutableCollection, "remove").checkOverridden(), new KotlinFunctionIntrinsic("collectionRemove"));
        add(pattern(mutableCollection, "clear").checkOverridden(), new KotlinFunctionIntrinsic("collectionClear"));
    }

    private final static class ValueParametersAwareDescriptorPredicate implements DescriptorPredicate {
        private final DescriptorPredicate predicate;
        private final int parameterCount;
        private final Predicate<JetType> firstParameterPredicate;

        public ValueParametersAwareDescriptorPredicate(@NotNull DescriptorPredicate predicate, @NotNull Predicate<JetType> firstParameterPredicate) {
            this.predicate = predicate;
            this.firstParameterPredicate = firstParameterPredicate;
            parameterCount = -1;
        }

        public ValueParametersAwareDescriptorPredicate(@NotNull DescriptorPredicate predicate, int parameterCount) {
            this.predicate = predicate;
            this.parameterCount = parameterCount;
            firstParameterPredicate = null;
        }

        @Override
        public boolean apply(@NotNull FunctionDescriptor descriptor) {
            if (!predicate.apply(descriptor)) {
                return false;
            }

            List<ValueParameterDescriptor> valueParameters = descriptor.getValueParameters();
            if (firstParameterPredicate == null) {
                assert parameterCount != -1;
                return valueParameters.size() == parameterCount;
            }
            else {
                return valueParameters.size() == 1 && firstParameterPredicate.apply(valueParameters.get(0).getType());
            }
        }
    }
}

/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.intrinsic;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.PrimitiveType;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.intrinsic.array.ArrayGetIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.array.ArraySetIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.array.BuiltInPropertyIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.array.CallStandardMethodIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.primitive.*;
import org.jetbrains.k2js.translate.intrinsic.string.CharAtIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.tuple.TupleAccessIntrinsic;
import org.jetbrains.k2js.translate.operation.OperatorTable;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.types.expressions.OperatorConventions.*;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getFunctionByName;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getPropertyByName;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Provides mechanism to substitute method calls /w native constucts directly.
 */
public final class Intrinsics {

    @NotNull
    private final Map<FunctionDescriptor, Intrinsic> functionIntrinsics =
            new HashMap<FunctionDescriptor, Intrinsic>();

    @NotNull
    private final Map<FunctionDescriptor, EqualsIntrinsic> equalsIntrinsics =
            new HashMap<FunctionDescriptor, EqualsIntrinsic>();

    @NotNull
    private final Map<FunctionDescriptor, CompareToIntrinsic> compareToIntrinsics =
            new HashMap<FunctionDescriptor, CompareToIntrinsic>();

    @NotNull
    private final Intrinsic lengthPropertyIntrinsic = new BuiltInPropertyIntrinsic("length");

    public static Intrinsics standardLibraryIntrinsics(@NotNull JetStandardLibrary library) {
        return new Intrinsics(library);
    }

    @NotNull
    private final JetStandardLibrary library;

    private Intrinsics(@NotNull JetStandardLibrary library) {
        this.library = library;
        declareOperatorIntrinsics();
        declareStringIntrinsics();
        declareTuplesIntrinsics();
        declareArrayIntrinsics();
    }

    @NotNull
    public Intrinsic getLengthPropertyIntrinsic() {
        return lengthPropertyIntrinsic;
    }

    private void declareTuplesIntrinsics() {
        for (int tupleSize = 0; tupleSize < JetStandardClasses.TUPLE_COUNT; ++tupleSize) {
            declareTupleIntrinsics(tupleSize);
        }
    }

    //TODO: provide generic mechanism or refactor
    private void declareArrayIntrinsics() {

        List<JetType> arrayTypes = getLibraryArrayTypes();

        for (JetType arrayType : arrayTypes) {
            declareIntrinsicsForArrayType(arrayType);
        }
        declareNullConstructorIntrinsic();
    }

    private void declareNullConstructorIntrinsic() {
        FunctionDescriptor nullArrayConstructor = library.getLibraryScope().getFunctions("Array").iterator().next();
        functionIntrinsics.put(nullArrayConstructor, new CallStandardMethodIntrinsic("Kotlin.nullArray", false, 1));
    }

    //TODO: some dangerous operation unchecked here
    private void declareIntrinsicsForArrayType(@NotNull JetType arrayType) {
        JetScope arrayMemberScope = arrayType.getMemberScope();
        FunctionDescriptor setFunction = getFunctionByName(arrayMemberScope, "set");
        functionIntrinsics.put(setFunction, ArraySetIntrinsic.INSTANCE);
        FunctionDescriptor getFunction = getFunctionByName(arrayMemberScope, "get");
        functionIntrinsics.put(getFunction, ArrayGetIntrinsic.INSTANCE);
        PropertyDescriptor sizeProperty = getPropertyByName(arrayMemberScope, "size");
        functionIntrinsics.put(sizeProperty.getGetter(), lengthPropertyIntrinsic);
        //TODO: excessive object creation
        PropertyDescriptor indicesProperty = getPropertyByName(arrayMemberScope, "indices");
        functionIntrinsics.put(indicesProperty.getGetter(), new CallStandardMethodIntrinsic("Kotlin.arrayIndices", true, 0));
        FunctionDescriptor iteratorFunction = getFunctionByName(arrayMemberScope, "iterator");
        functionIntrinsics.put(iteratorFunction, new CallStandardMethodIntrinsic("Kotlin.arrayIterator", true, 0));
        ConstructorDescriptor arrayConstructor =
                ((ClassDescriptor) arrayMemberScope.getContainingDeclaration()).getConstructors().iterator().next();
        functionIntrinsics.put(arrayConstructor, new CallStandardMethodIntrinsic("Kotlin.arrayFromFun", false, 2));
    }

    private List<JetType> getLibraryArrayTypes() {
        List<JetType> arrayTypes = Lists.newArrayList();
        for (PrimitiveType type : PrimitiveType.values()) {
            arrayTypes.add(library.getPrimitiveArrayJetType(type));
        }
        arrayTypes.add(library.getArray().getDefaultType());
        return arrayTypes;
    }

    private void declareTupleIntrinsics(int tupleSize) {
        JetScope libraryScope = library.getLibraryScope();
        assert libraryScope != null;
        ClassifierDescriptor tupleDescriptor = libraryScope.getClassifier("Tuple" + tupleSize);
        assert tupleDescriptor != null;
        declareTupleIntrinsicAccessors(tupleDescriptor, tupleSize);
    }

    private void declareStringIntrinsics() {
        PropertyDescriptor lengthProperty =
                getPropertyByName(library.getCharSequence().getDefaultType().getMemberScope(), "length");
        functionIntrinsics.put(lengthProperty.getGetter(), new BuiltInPropertyIntrinsic("length"));
        FunctionDescriptor getFunction =
                getFunctionByName(library.getString().getDefaultType().getMemberScope(), "get");
        functionIntrinsics.put(getFunction, CharAtIntrinsic.INSTANCE);
    }

    private void declareTupleIntrinsicAccessors(@NotNull ClassifierDescriptor tupleDescriptor,
                                                int tupleSize) {
        for (int elementIndex = 0; elementIndex < tupleSize; ++elementIndex) {
            String accessorName = "_" + (elementIndex + 1);
            PropertyDescriptor propertyDescriptor =
                    getPropertyByName(tupleDescriptor.getDefaultType().getMemberScope(), accessorName);
            functionIntrinsics.put(propertyDescriptor.getGetter(), new TupleAccessIntrinsic(elementIndex));
        }
    }

    private void declareOperatorIntrinsics() {
        IntrinsicDeclarationVisitor visitor = new IntrinsicDeclarationVisitor();
        for (DeclarationDescriptor descriptor : library.getLibraryScope().getAllDescriptors()) {
            //noinspection NullableProblems
            descriptor.accept(visitor, null);
        }
    }

    public boolean isIntrinsic(@NotNull DeclarationDescriptor descriptor) {
        //NOTE: that if we want to add other intrinsics we have to modify this method
        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor.getOriginal();
            return (equalsIntrinsics.containsKey(functionDescriptor) ||
                    compareToIntrinsics.containsKey(functionDescriptor) ||
                    functionIntrinsics.containsKey(functionDescriptor));
        }
        return false;
    }

    @NotNull
    public Intrinsic getFunctionIntrinsic(@NotNull FunctionDescriptor descriptor) {
        return functionIntrinsics.get(descriptor.getOriginal());
    }

    @NotNull
    public CompareToIntrinsic getCompareToIntrinsic(@NotNull FunctionDescriptor descriptor) {
        return compareToIntrinsics.get(descriptor.getOriginal());
    }

    @NotNull
    public EqualsIntrinsic getEqualsIntrinsic(@NotNull FunctionDescriptor descriptor) {
        return equalsIntrinsics.get(descriptor.getOriginal());
    }

    @SuppressWarnings("ConstantConditions")
    private final class IntrinsicDeclarationVisitor extends DeclarationDescriptorVisitor<Void, Void> {

        @Override
        public Void visitClassDescriptor(@NotNull ClassDescriptor descriptor, @Nullable Void nothing) {
            for (DeclarationDescriptor memberDescriptor :
                    descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
                //noinspection NullableProblems
                memberDescriptor.accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitFunctionDescriptor(@NotNull FunctionDescriptor descriptor, @Nullable Void nothing) {
            if (!isIntrinsic(descriptor)) {
                declareOperatorIntrinsic(descriptor);
            }
            return null;
        }


        /*package*/ void declareOperatorIntrinsic(@NotNull FunctionDescriptor descriptor) {
            tryResolveAsEqualsCompareToOrRangeToIntrinsic(descriptor);
            tryResolveAsUnaryIntrinsics(descriptor);
            tryResolveAsBinaryIntrinsics(descriptor);
        }

        private void tryResolveAsEqualsCompareToOrRangeToIntrinsic(@NotNull FunctionDescriptor descriptor) {
            String functionName = descriptor.getName();
            if (functionName.equals(COMPARE_TO)) {
                compareToIntrinsics.put(descriptor, PrimitiveCompareToIntrinsic.newInstance());
            }
            if (functionName.equals(EQUALS)) {
                equalsIntrinsics.put(descriptor, PrimitiveEqualsIntrinsic.newInstance());
            }
            if (functionName.equals("rangeTo")) {
                functionIntrinsics.put(descriptor, PrimitiveRangeToIntrinsic.newInstance());
            }
        }

        private void tryResolveAsUnaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
            String functionName = descriptor.getName();
            JetToken token = UNARY_OPERATION_NAMES.inverse().get(functionName);

            if (token == null) return;
            if (!isUnaryOperation(descriptor)) return;

            functionIntrinsics.put(descriptor, PrimitiveUnaryOperationIntrinsic.newInstance(token));
        }

        private void tryResolveAsBinaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
            String functionName = descriptor.getName();

            if (isUnaryOperation(descriptor)) return;

            JetToken token = BINARY_OPERATION_NAMES.inverse().get(functionName);
            if (token == null) return;

            if (!OperatorTable.hasCorrespondingBinaryOperator(token)) return;
            functionIntrinsics.put(descriptor, PrimitiveBinaryOperationIntrinsic.newInstance(token));
        }

        private boolean isUnaryOperation(@NotNull FunctionDescriptor descriptor) {
            return !DescriptorUtils.hasParameters(descriptor);
        }
    }

}

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

package org.jetbrains.k2js.translate.intrinsic;

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.intrinsic.array.ArrayGetIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.array.ArraySetIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.primitive.*;
import org.jetbrains.k2js.translate.intrinsic.string.CharAtIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.tuple.TupleAccessIntrinsic;
import org.jetbrains.k2js.translate.operation.OperatorTable;
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.types.expressions.OperatorConventions.*;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getFunctionByName;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getPropertyByName;

//TODO: class is monstrous, should be refactored.

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
        declareBooleanIntrinsics();
        FunctionDescriptor intToDouble = getFunctionByName(library.getInt().getDefaultType().getMemberScope(), Name.identifier("toDouble"));
        functionIntrinsics.put(intToDouble, ReturnReceiverIntrinsic.INSTANCE);
        FunctionDescriptor doubleToInt = getFunctionByName(library.getDouble().getDefaultType().getMemberScope(), Name.identifier("toInt"));
        functionIntrinsics.put(doubleToInt, new CallStandardMethodIntrinsic("Math.floor", true, 0));
        FunctionDescriptor toStringFunction = getFunctionByName(library.getLibraryScope(), Name.identifier("toString"));
        functionIntrinsics.put(toStringFunction, new BuiltInFunctionIntrinsic("toString"));
    }

    private void declareBooleanIntrinsics() {
        FunctionDescriptor andFunction = getFunctionByName(library.getBoolean().getDefaultType().getMemberScope(), Name.identifier("and"));
        functionIntrinsics.put(andFunction, PrimitiveBinaryOperationIntrinsic.newInstance(JetTokens.ANDAND));

        FunctionDescriptor orFunction = getFunctionByName(library.getBoolean().getDefaultType().getMemberScope(), Name.identifier("or"));
        functionIntrinsics.put(orFunction, PrimitiveBinaryOperationIntrinsic.newInstance(JetTokens.OROR));

        FunctionDescriptor notFunction = getFunctionByName(library.getBoolean().getDefaultType().getMemberScope(), Name.identifier("not"));
        functionIntrinsics.put(notFunction, PrimitiveUnaryOperationIntrinsic.newInstance(JetTokens.EXCL));

        FunctionDescriptor xorFunction = getFunctionByName(library.getBoolean().getDefaultType().getMemberScope(), Name.identifier("xor"));
        functionIntrinsics.put(xorFunction, PrimitiveBinaryOperationIntrinsic.newInstance(JsBinaryOperator.BIT_XOR));
    }

    @NotNull
    public Intrinsic getLengthPropertyIntrinsic() {
        return lengthPropertyIntrinsic;
    }

    private void declareTuplesIntrinsics() {
        for (int tupleSize = 0; tupleSize <= JetStandardClasses.MAX_TUPLE_ORDER; ++tupleSize) {
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
        FunctionDescriptor nullArrayConstructor = library.getLibraryScope().getFunctions(Name.identifier("arrayOfNulls")).iterator().next();
        functionIntrinsics.put(nullArrayConstructor, new CallStandardMethodIntrinsic("Kotlin.nullArray", false, 1));
    }

    //TODO: some dangerous operation unchecked here
    private void declareIntrinsicsForArrayType(@NotNull JetType arrayType) {
        JetScope arrayMemberScope = arrayType.getMemberScope();
        FunctionDescriptor setFunction = getFunctionByName(arrayMemberScope, Name.identifier("set"));
        functionIntrinsics.put(setFunction, ArraySetIntrinsic.INSTANCE);
        FunctionDescriptor getFunction = getFunctionByName(arrayMemberScope, Name.identifier("get"));
        functionIntrinsics.put(getFunction, ArrayGetIntrinsic.INSTANCE);
        PropertyDescriptor sizeProperty = getPropertyByName(arrayMemberScope, Name.identifier("size"));
        functionIntrinsics.put(sizeProperty.getGetter(), lengthPropertyIntrinsic);
        //TODO: excessive object creation
        PropertyDescriptor indicesProperty = getPropertyByName(arrayMemberScope, Name.identifier("indices"));
        functionIntrinsics.put(indicesProperty.getGetter(), new CallStandardMethodIntrinsic("Kotlin.arrayIndices", true, 0));
        FunctionDescriptor iteratorFunction = getFunctionByName(arrayMemberScope, Name.identifier("iterator"));
        functionIntrinsics.put(iteratorFunction, new CallStandardMethodIntrinsic("Kotlin.arrayIterator", true, 0));
        ConstructorDescriptor arrayConstructor =
            ((ClassDescriptor)arrayMemberScope.getContainingDeclaration()).getConstructors().iterator().next();
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
        ClassifierDescriptor tupleDescriptor = libraryScope.getClassifier(Name.identifier("Tuple" + tupleSize));
        assert tupleDescriptor != null;
        declareTupleIntrinsicAccessors(tupleDescriptor, tupleSize);
    }

    private void declareStringIntrinsics() {
        //TODO: same intrinsic for 2 different methods
        PropertyDescriptor stringLengthProperty =
            getPropertyByName(library.getString().getDefaultType().getMemberScope(), Name.identifier("length"));
        functionIntrinsics.put(stringLengthProperty.getGetter(), lengthPropertyIntrinsic);
        PropertyDescriptor charSequenceLengthProperty =
            getPropertyByName(library.getCharSequence().getDefaultType().getMemberScope(), Name.identifier("length"));
        functionIntrinsics.put(charSequenceLengthProperty.getGetter(), lengthPropertyIntrinsic);
        FunctionDescriptor getFunction =
            getFunctionByName(library.getString().getDefaultType().getMemberScope(), Name.identifier("get"));
        functionIntrinsics.put(getFunction, CharAtIntrinsic.INSTANCE);
    }

    private void declareTupleIntrinsicAccessors(@NotNull ClassifierDescriptor tupleDescriptor,
                                                int tupleSize) {
        for (int elementIndex = 0; elementIndex < tupleSize; ++elementIndex) {
            Name accessorName = Name.identifier("_" + (elementIndex + 1));
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
            FunctionDescriptor functionDescriptor = (FunctionDescriptor)descriptor.getOriginal();
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
            Name functionName = descriptor.getName();
            if (functionName.equals(COMPARE_TO)) {
                compareToIntrinsics.put(descriptor, PrimitiveCompareToIntrinsic.newInstance());
            }
            if (functionName.equals(EQUALS)) {
                equalsIntrinsics.put(descriptor, PrimitiveEqualsIntrinsic.newInstance());
            }
            if (functionName.equals(Name.identifier("rangeTo"))) {
                functionIntrinsics.put(descriptor, PrimitiveRangeToIntrinsic.newInstance());
            }
        }

        private void tryResolveAsUnaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
            Name functionName = descriptor.getName();
            JetToken token = UNARY_OPERATION_NAMES.inverse().get(functionName);

            if (token == null) return;
            if (!isUnaryOperation(descriptor)) return;

            functionIntrinsics.put(descriptor, PrimitiveUnaryOperationIntrinsic.newInstance(token));
        }

        private void tryResolveAsBinaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
            Name functionName = descriptor.getName();

            if (isUnaryOperation(descriptor)) return;

            JetToken token = BINARY_OPERATION_NAMES.inverse().get(functionName);
            if (token == null) return;

            if (!OperatorTable.hasCorrespondingBinaryOperator(token)) return;
            functionIntrinsics.put(descriptor, PrimitiveBinaryOperationIntrinsic.newInstance(token));
        }

        private boolean isUnaryOperation(@NotNull FunctionDescriptor descriptor) {
            return !JsDescriptorUtils.hasParameters(descriptor);
        }
    }
}

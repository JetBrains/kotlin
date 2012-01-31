package org.jetbrains.k2js.translate.intrinsic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.intrinsic.array.*;
import org.jetbrains.k2js.translate.intrinsic.primitive.*;
import org.jetbrains.k2js.translate.intrinsic.string.LengthIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.tuple.TupleAccessIntrinsic;
import org.jetbrains.k2js.translate.operation.OperatorTable;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lang.types.expressions.OperatorConventions.*;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getFunctionByName;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getPropertyByName;

/**
 * @author Pavel Talanov
 */
public final class Intrinsics {

    @NotNull
    private final Map<FunctionDescriptor, FunctionIntrinsic> functionIntrinsics =
            new HashMap<FunctionDescriptor, FunctionIntrinsic>();

    @NotNull
    private final Map<FunctionDescriptor, EqualsIntrinsic> equalsIntrinsics =
            new HashMap<FunctionDescriptor, EqualsIntrinsic>();

    @NotNull
    private final Map<FunctionDescriptor, CompareToIntrinsic> compareToIntrinsics =
            new HashMap<FunctionDescriptor, CompareToIntrinsic>();

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

    //TODO: provide generic mechanism or refactor
    private void declareArrayIntrinsics() {
        JetScope arrayMemberScope = library.getArray().getDefaultType().getMemberScope();
        FunctionDescriptor setFunction = getFunctionByName(arrayMemberScope, "set");
        functionIntrinsics.put(setFunction, ArraySetIntrinsic.INSTANCE);
        FunctionDescriptor getFunction = getFunctionByName(arrayMemberScope, "get");
        functionIntrinsics.put(getFunction, ArrayGetIntrinsic.INSTANCE);
        PropertyDescriptor sizeProperty = getPropertyByName(arrayMemberScope, "size");
        functionIntrinsics.put(sizeProperty.getGetter(), ArraySizeIntrinsic.INSTANCE);
        PropertyDescriptor indicesProperty = getPropertyByName(arrayMemberScope, "indices");
        functionIntrinsics.put(indicesProperty.getGetter(), ArrayIndicesIntrinsic.INSTANCE);
        FunctionDescriptor nullArrayConstructor = getFunctionByName(library.getLibraryScope(), "Array");
        functionIntrinsics.put(nullArrayConstructor, ArrayNullConstructorIntrinsic.INSTANCE);
        FunctionDescriptor iteratorFunction = getFunctionByName(arrayMemberScope, "iterator");
        functionIntrinsics.put(iteratorFunction, ArrayIteratorIntrinsic.INSTANCE);
        ConstructorDescriptor arrayConstructor = library.getArray().getConstructors().iterator().next();
        functionIntrinsics.put(arrayConstructor, ArrayFunctionConstructorIntrinsic.INSTANCE);
    }

    private void declareTuplesIntrinsics() {
        for (int tupleSize = 0; tupleSize < JetStandardClasses.TUPLE_COUNT; ++tupleSize) {
            declareTupleIntrinsics(tupleSize);
        }
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
                getPropertyByName(library.getString().getDefaultType().getMemberScope(), "length");
        functionIntrinsics.put(lengthProperty.getGetter(), LengthIntrinsic.INSTANCE);
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
    public FunctionIntrinsic getFunctionIntrinsic(@NotNull FunctionDescriptor descriptor) {
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

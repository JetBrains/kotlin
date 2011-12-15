package org.jetbrains.k2js.translate.intrinsic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.intrinsic.array.ArrayGetIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.array.ArrayNullConstructorIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.array.ArraySetIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.primitive.*;
import org.jetbrains.k2js.translate.operation.OperatorTable;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lang.types.expressions.OperatorConventions.*;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getFunctionByName;

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
        //TODO: array intrinsic are under consideration
        //declareArrayIntrinsics();
    }

    private void declareOperatorIntrinsics() {
        IntrinsicDeclarationVisitor visitor = new IntrinsicDeclarationVisitor();
        for (DeclarationDescriptor descriptor : library.getLibraryScope().getAllDescriptors()) {
            //noinspection NullableProblems
            descriptor.accept(visitor, null);
        }
    }

    //TODO: delete or include
    private void declareArrayIntrinsics() {
        FunctionDescriptor constructorFunction = getFunctionByName(library.getLibraryScope(), "Array");
        functionIntrinsics.put(constructorFunction, ArrayNullConstructorIntrinsic.INSTANCE);

        FunctionDescriptor getFunction = getFunctionByName(library.getArray(), "get");
        functionIntrinsics.put(getFunction, ArrayGetIntrinsic.INSTANCE);

        FunctionDescriptor setFunction = getFunctionByName(library.getArray(), "set");
        functionIntrinsics.put(setFunction, ArraySetIntrinsic.INSTANCE);
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

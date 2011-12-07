package org.jetbrains.k2js.translate.intrinsic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.intrinsic.array.ArrayGetIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.array.ArrayNullConstructorIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.array.ArraySetIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.primitive.PrimitiveBinaryOperationIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.primitive.PrimitiveCompareToIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.primitive.PrimitiveEqualsIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.primitive.PrimitiveUnaryOperationIntrinsic;
import org.jetbrains.k2js.translate.operation.OperatorTable;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getFunctionByName;

/**
 * @author Talanov Pavel
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
        IntrinsicDeclarationVisitor visitor = new IntrinsicDeclarationVisitor(this);
        for (DeclarationDescriptor descriptor : library.getLibraryScope().getAllDescriptors()) {
            descriptor.accept(visitor, null);
        }
    }

    private void declareArrayIntrinsics() {
        FunctionDescriptor constructorFunction = getFunctionByName(library.getLibraryScope(), "Array");
        functionIntrinsics.put(constructorFunction, ArrayNullConstructorIntrinsic.INSTANCE);

        FunctionDescriptor getFunction = getFunctionByName(library.getArray(), "get");
        functionIntrinsics.put(getFunction, ArrayGetIntrinsic.INSTANCE);

        FunctionDescriptor setFunction = getFunctionByName(library.getArray(), "set");
        functionIntrinsics.put(setFunction, ArraySetIntrinsic.INSTANCE);
    }

    /*package*/ void declareOperatorIntrinsic(@NotNull FunctionDescriptor descriptor) {
        addCompareToIntrinsics(descriptor);
        addEqualsIntrinsics(descriptor);
        addUnaryIntrinsics(descriptor);
        addBinaryIntrinsics(descriptor);
    }

    private void addCompareToIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        if (functionName.equals("compareTo")) {
            compareToIntrinsics.put(descriptor, PrimitiveCompareToIntrinsic.newInstance());
        }
    }

    private void addEqualsIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        if (functionName.equals("equals")) {
            equalsIntrinsics.put(descriptor, PrimitiveEqualsIntrinsic.newInstance());
        }
    }

    private void addUnaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        JetToken token = OperatorConventions.UNARY_OPERATION_NAMES.inverse().get(functionName);
        if (token == null) return;
        boolean isUnary = !DescriptorUtils.hasParameters(descriptor);
        if (!isUnary) return;
        functionIntrinsics.put(descriptor, PrimitiveUnaryOperationIntrinsic.newInstance(token));
    }

    //TODO: refactor
    private void addBinaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        boolean isUnary = !DescriptorUtils.hasParameters(descriptor);
        if (isUnary) return;
        JetToken token = OperatorConventions.BINARY_OPERATION_NAMES.inverse().get(functionName);
        if (token == null) return;
        //TODO: implement range and contains intrinsic
        if (!OperatorTable.hasCorrespondingBinaryOperator(token)) return;
        functionIntrinsics.put(descriptor, PrimitiveBinaryOperationIntrinsic.newInstance(token));
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
}

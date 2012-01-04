package org.jetbrains.k2js.translate.utils;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getVariableDescriptorForVariableAsFunction;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.isVariableDescriptor;

/**
 * @author Pavel Talanov
 *         <p/>
 *         This class contains some code related to BindingContext use. Intention is not to pollute other classes.
 */
public final class BindingUtils {

    private BindingUtils() {
    }


    @NotNull
    static private <E extends PsiElement, D extends DeclarationDescriptor>
    D getDescriptorForExpression(@NotNull BindingContext context, @NotNull E expression, Class<D> descriptorClass) {
        DeclarationDescriptor descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, expression);
        assert descriptor != null;
        assert descriptorClass.isInstance(descriptor)
                : expression.toString() + " expected to have of type" + descriptorClass.toString();
        //noinspection unchecked
        return (D) descriptor;
    }

    @NotNull
    public static ClassDescriptor getClassDescriptor(@NotNull BindingContext context, @NotNull JetClass declaration) {
        return getDescriptorForExpression(context, declaration, ClassDescriptor.class);
    }

    @NotNull
    public static NamespaceDescriptor getNamespaceDescriptor(@NotNull BindingContext context,
                                                             @NotNull JetFile declaration) {
        return getDescriptorForExpression(context, declaration, NamespaceDescriptor.class);
    }

    @NotNull
    public static FunctionDescriptor getFunctionDescriptor(@NotNull BindingContext context,
                                                           @NotNull JetDeclarationWithBody declaration) {
        return getDescriptorForExpression(context, declaration, FunctionDescriptor.class);
    }

    @NotNull
    public static PropertyAccessorDescriptor getPropertyAccessorDescriptor(@NotNull BindingContext context,
                                                                           @NotNull JetPropertyAccessor declaration) {
        return getDescriptorForExpression(context, declaration, PropertyAccessorDescriptor.class);
    }

    @NotNull
    public static PropertyDescriptor getPropertyDescriptor(@NotNull BindingContext context,
                                                           @NotNull JetProperty declaration) {
        return getDescriptorForExpression(context, declaration, PropertyDescriptor.class);
    }

    @NotNull
    public static JetClass getClassForDescriptor(@NotNull BindingContext context,
                                                 @NotNull ClassDescriptor descriptor) {
        PsiElement result = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        assert result instanceof JetClass : "ClassDescriptor should have declaration of type JetClass";
        return (JetClass) result;
    }

    @NotNull
    public static List<JetDeclaration> getDeclarationsForNamespace(@NotNull BindingContext bindingContext,
                                                                   @NotNull NamespaceDescriptor namespace) {
        List<JetDeclaration> declarations = new ArrayList<JetDeclaration>();
        for (DeclarationDescriptor descriptor : namespace.getMemberScope().getAllDescriptors()) {
            JetDeclaration declaration = BindingUtils.getDeclarationForDescriptor(bindingContext, descriptor);
            if (declaration != null) {
                declarations.add(declaration);
            }
        }
        return declarations;
    }

    @Nullable
    private static JetDeclaration getDeclarationForDescriptor(@NotNull BindingContext context,
                                                              @NotNull DeclarationDescriptor descriptor) {
        PsiElement result = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        if (result == null) {
            return null;
        }
        assert result instanceof JetDeclaration : "Descriptor should correspond to an element.";
        return (JetDeclaration) result;
    }

    @NotNull
    public static List<ClassDescriptor> getSuperclassDescriptors(@NotNull BindingContext context,
                                                                 @NotNull JetClass classDeclaration) {
        ClassDescriptor classDescriptor = getClassDescriptor(context, classDeclaration);
        return getSuperclassDescriptors(classDescriptor);
    }

    @NotNull
    public static List<ClassDescriptor> getSuperclassDescriptors(@NotNull ClassDescriptor classDescriptor) {
        Collection<? extends JetType> superclassTypes = classDescriptor.getTypeConstructor().getSupertypes();
        List<ClassDescriptor> superClassDescriptors = new ArrayList<ClassDescriptor>();
        for (JetType type : superclassTypes) {
            ClassDescriptor result = getClassDescriptorForType(type);
            if (isNotAny(result)) {
                superClassDescriptors.add(result);
            }
        }
        return superClassDescriptors;
    }

    @NotNull
    private static ClassDescriptor getClassDescriptorForType(@NotNull JetType type) {
        DeclarationDescriptor superClassDescriptor =
                type.getConstructor().getDeclarationDescriptor();
        assert superClassDescriptor instanceof ClassDescriptor
                : "Superclass descriptor of a type should be of type ClassDescriptor";
        return (ClassDescriptor) superClassDescriptor;
    }

    public static boolean hasAncestorClass(@NotNull BindingContext context, @NotNull JetClass classDeclaration) {
        List<ClassDescriptor> superclassDescriptors = getSuperclassDescriptors(context, classDeclaration);
        return (DescriptorUtils.findAncestorClass(superclassDescriptors) != null);
    }

    public static boolean isStatement(@NotNull BindingContext context, @NotNull JetExpression expression) {
        Boolean isStatement = context.get(BindingContext.STATEMENT, expression);
        assert isStatement != null : "Invalid behaviour of get(BindingContext.STATEMENT)";
        return isStatement;
        // return IsStatement.isStatement(expression);
    }

    @NotNull
    public static JetType getTypeByReference(@NotNull BindingContext context,
                                             @NotNull JetTypeReference typeReference) {
        JetType result = context.get(BindingContext.TYPE, typeReference);
        assert result != null : "TypeReference should reference a type";
        return result;
    }

    @NotNull
    public static ClassDescriptor getClassDescriptorForTypeReference(@NotNull BindingContext context,
                                                                     @NotNull JetTypeReference typeReference) {
        return getClassDescriptorForType(getTypeByReference(context, typeReference));
    }

    @Nullable
    public static PropertyDescriptor getPropertyDescriptorForConstructorParameter(@NotNull BindingContext context,
                                                                                  @NotNull JetParameter parameter) {
        return context.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
    }

    @Nullable
    public static JetProperty getPropertyForDescriptor(@NotNull BindingContext context,
                                                       @NotNull PropertyDescriptor property) {
        PsiElement result = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, property);
        if (!(result instanceof JetProperty)) {
            return null;
        }
        return (JetProperty) result;
    }

    @NotNull
    public static DeclarationDescriptor getDescriptorForReferenceExpression(@NotNull BindingContext context,
                                                                            @NotNull JetReferenceExpression reference) {
        DeclarationDescriptor referencedDescriptor = getNullableDescriptorForReferenceExpression(context, reference);
        assert referencedDescriptor != null : "Reference expression must reference a descriptor.";
        return referencedDescriptor;
    }

    @Nullable
    private static DeclarationDescriptor getNullableDescriptorForReferenceExpression(@NotNull BindingContext context,
                                                                                     @NotNull JetReferenceExpression reference) {
        DeclarationDescriptor referencedDescriptor = context.get(BindingContext.REFERENCE_TARGET, reference);
        if (isVariableDescriptor(referencedDescriptor)) {
            assert referencedDescriptor != null;
            return getVariableDescriptorForVariableAsFunction((VariableAsFunctionDescriptor) referencedDescriptor);
        }
        return referencedDescriptor;
    }

    private static boolean isNotAny(@NotNull DeclarationDescriptor superClassDescriptor) {
        return !superClassDescriptor.equals(JetStandardClasses.getAny());
    }

    public static boolean isOwnedByNamespace(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ConstructorDescriptor) {
            DeclarationDescriptor classDescriptor = descriptor.getContainingDeclaration();
            assert classDescriptor != null;
            return isOwnedByNamespace(classDescriptor);
        }
        return (descriptor.getContainingDeclaration() instanceof NamespaceDescriptor);
    }

    public static boolean isOwnedByClass(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ConstructorDescriptor) {
            DeclarationDescriptor classDescriptor = descriptor.getContainingDeclaration();
            assert classDescriptor != null;
            return isOwnedByClass(classDescriptor);
        }
        return (descriptor.getContainingDeclaration() instanceof ClassDescriptor);
    }

    @NotNull
    private static ResolvedCall<?> getResolvedCall(@NotNull BindingContext context,
                                                   @NotNull JetExpression expression) {
        ResolvedCall<? extends CallableDescriptor> resolvedCall = context.get(BindingContext.RESOLVED_CALL, expression);
        assert resolvedCall != null : "Must resolve to a call.";
        return resolvedCall;
    }

    @NotNull
    public static FunctionDescriptor getFunctionDescriptorForCallExpression(@NotNull BindingContext context,
                                                                            @NotNull JetCallExpression expression) {
        JetExpression calleeExpression = PsiUtils.getCallee(expression);
        ResolvedCall<?> resolvedCall = getResolvedCall(context, calleeExpression);
        CallableDescriptor descriptor = getDescriptorForResolvedCall(resolvedCall);
        assert descriptor instanceof FunctionDescriptor :
                "Callee expression must have resolved call with descriptor of type FunctionDescriptor";
        return (FunctionDescriptor) descriptor;
    }

    @NotNull
    private static CallableDescriptor getDescriptorForResolvedCall(@NotNull ResolvedCall<?> resolvedCall) {
        return resolvedCall.getCandidateDescriptor();
    }

    public static boolean isVariableReassignment(@NotNull BindingContext context, @NotNull JetExpression expression) {
        Boolean result = context.get(BindingContext.VARIABLE_REASSIGNMENT, expression);
        assert result != null;
        return result;
    }


    @Nullable
    public static FunctionDescriptor getFunctionDescriptorForOperationExpression(@NotNull BindingContext context,
                                                                                 @NotNull JetOperationExpression expression) {
        DeclarationDescriptor descriptorForReferenceExpression = getNullableDescriptorForReferenceExpression
                (context, expression.getOperationReference());

        if (descriptorForReferenceExpression == null) return null;

        assert descriptorForReferenceExpression instanceof FunctionDescriptor
                : "Operation should resolve to function descriptor.";
        return (FunctionDescriptor) descriptorForReferenceExpression;
    }

    @NotNull
    public static DeclarationDescriptor getDescriptorForElement(@NotNull BindingContext context,
                                                                @NotNull JetElement element) {
        DeclarationDescriptor descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        assert descriptor != null : element + " doesn't have a descriptor.";
        return descriptor;
    }

    @Nullable
    public static Object getCompileTimeValue(@NotNull BindingContext context, @NotNull JetExpression expression) {
        CompileTimeConstant<?> compileTimeValue = context.get(BindingContext.COMPILE_TIME_VALUE, expression);
        if (compileTimeValue != null) {
            return compileTimeValue.getValue();
        }
        return null;
    }

}

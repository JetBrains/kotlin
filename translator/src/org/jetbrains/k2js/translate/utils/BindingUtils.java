package org.jetbrains.k2js.translate.utils;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Talanov Pavel
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
        return (D) descriptor;
    }

    @NotNull
    public static ClassDescriptor getClassDescriptor(@NotNull BindingContext context, @NotNull JetClass declaration) {
        return getDescriptorForExpression(context, declaration, ClassDescriptor.class);
    }

    @NotNull
    public static NamespaceDescriptor getNamespaceDescriptor(@NotNull BindingContext context,
                                                             @NotNull JetNamespace declaration) {
        return getDescriptorForExpression(context, declaration, NamespaceDescriptor.class);
    }

    @NotNull
    public static FunctionDescriptor getFunctionDescriptor(@NotNull BindingContext context,
                                                           @NotNull JetNamedFunction declaration) {
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

    //TODO: possibly remove
//    @NotNull
//    public static PropertySetterDescriptor getPropertySetterDescriptorForProperty(@NotNull BindingContext context,
//                                                                                  @NotNull JetProperty property) {
//        PropertySetterDescriptor result = getPropertyDescriptor(context, property).getSetter();
//        assert result != null : "Property should have a setter descriptor";
//        return result;
//    }
//
//    @NotNull
//    public static PropertyGetterDescriptor getPropertyGetterDescriptorForProperty(@NotNull BindingContext context,
//                                                                                  @NotNull JetProperty property) {
//        PropertyGetterDescriptor result = getPropertyDescriptor(context, property).getGetter();
//        assert result != null : "Property should have a getter descriptor";
//        return result;
//    }

    @NotNull
    public static JetClass getClassForDescriptor(@NotNull BindingContext context,
                                                 @NotNull ClassDescriptor descriptor) {
        PsiElement result = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        assert result instanceof JetClass : "ClassDescriptor should have declaration of type JetClass";
        return (JetClass) result;
    }

    //TODO: delete?
    @Nullable
    public static JetDeclaration getDeclarationForDescriptor(@NotNull BindingContext context,
                                                             @NotNull DeclarationDescriptor descriptor) {
        PsiElement result = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        if (!(result instanceof JetDeclaration)) {
            return null;
        }
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
    public static ClassDescriptor getClassDescriptorForType(@NotNull JetType type) {
        DeclarationDescriptor superClassDescriptor =
                type.getConstructor().getDeclarationDescriptor();
        assert superClassDescriptor instanceof ClassDescriptor
                : "Superclass descriptor of a type should be of type ClassDescriptor";
        return (ClassDescriptor) superClassDescriptor;
    }

    public static boolean hasAncestorClass(@NotNull BindingContext context, @NotNull JetClass classDeclaration) {
        List<ClassDescriptor> superclassDescriptors = getSuperclassDescriptors(context, classDeclaration);
        return (findAncestorClass(superclassDescriptors) != null);
    }

    public static boolean isStatement(@NotNull BindingContext context, @NotNull JetExpression expression) {
        Boolean isStatement = context.get(BindingContext.STATEMENT, expression);
        assert isStatement != null : "Invalid behaviour of get(BindingContext.STATEMENT)";
        return isStatement;
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

    @Nullable
    public static DeclarationDescriptor getDescriptorForReferenceExpression(@NotNull BindingContext context,
                                                                            @NotNull JetReferenceExpression reference) {
        return context.get(BindingContext.REFERENCE_TARGET, reference);
    }

    static private boolean isNotAny(@NotNull DeclarationDescriptor superClassDescriptor) {
        return !superClassDescriptor.equals(JetStandardClasses.getAny());
    }

    //TODO move unrelated utils to other class
    @Nullable
    public static ClassDescriptor findAncestorClass(@NotNull List<ClassDescriptor> superclassDescriptors) {
        for (ClassDescriptor descriptor : superclassDescriptors) {
            if (descriptor.getKind() == ClassKind.CLASS) {
                return descriptor;
            }
        }
        return null;
    }

    public static boolean isOwnedByNamespace(@NotNull DeclarationDescriptor descriptor) {
        return (descriptor.getContainingDeclaration() instanceof NamespaceDescriptor);
    }

    public static boolean isOwnedByClass(@NotNull DeclarationDescriptor descriptor) {
        return (descriptor.getContainingDeclaration() instanceof ClassDescriptor);
    }


    //TODO: refactor, check with getDescriptorForReferenceExpression
    @Nullable
    public static PropertyDescriptor getPropertyDescriptorForSimpleName(@NotNull BindingContext context,
                                                                        @NotNull JetSimpleNameExpression expression) {
        ResolvedCall<?> resolvedCall = BindingUtils.getResolvedCall(context, expression);
        if (resolvedCall == null) return null;

        DeclarationDescriptor descriptor = resolvedCall.getCandidateDescriptor();
        if (descriptor instanceof PropertyDescriptor) {
            return (PropertyDescriptor) descriptor;
        }
        if (descriptor instanceof VariableAsFunctionDescriptor) {
            VariableAsFunctionDescriptor functionVariable = (VariableAsFunctionDescriptor) descriptor;
            VariableDescriptor variableDescriptor = functionVariable.getVariableDescriptor();
            if (variableDescriptor instanceof PropertyDescriptor) {
                return (PropertyDescriptor) variableDescriptor;
            }
        }
        return null;
    }

    @Nullable
    public static ResolvedCall<?> getResolvedCall(@NotNull BindingContext context,
                                                  @NotNull JetExpression expression) {
        return (context.get(BindingContext.RESOLVED_CALL, expression));
    }

    @NotNull
    public static FunctionDescriptor getFunctionDescriptorForCallExpression(@NotNull BindingContext context,
                                                                            @NotNull JetCallExpression expression) {
        //TODO: move to PSI utils
        JetExpression calleeExpression = expression.getCalleeExpression();
        assert calleeExpression != null;
        ResolvedCall<?> resolvedCall = getResolvedCall(context, calleeExpression);
        //TODO
        CallableDescriptor descriptor = resolvedCall.getCandidateDescriptor();
        assert descriptor instanceof FunctionDescriptor :
                "Callee expression must have resolved call with descriptor of type FunctionDescriptor";
        return (FunctionDescriptor) descriptor;
    }

    public static boolean isVariableReassignment(@NotNull BindingContext context, @NotNull JetExpression expression) {
        Boolean result = context.get(BindingContext.VARIABLE_REASSIGNMENT, expression);
        assert result != null;
        return result;
    }


    @Nullable
    public static FunctionDescriptor getFunctionDescriptorForOperationExpression(@NotNull BindingContext context,
                                                                                 @NotNull JetOperationExpression expression) {
        DeclarationDescriptor descriptorForReferenceExpression = getDescriptorForReferenceExpression
                (context, expression.getOperation());

        if (descriptorForReferenceExpression == null) return null;

        assert descriptorForReferenceExpression instanceof FunctionDescriptor
                : "Operation should resolve to function descriptor.";
        return (FunctionDescriptor) descriptorForReferenceExpression;
    }

}

package org.jetbrains.k2js.translate;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
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
    static public ClassDescriptor getClassDescriptor(@NotNull BindingContext context, @NotNull JetClass declaration) {
        return getDescriptorForExpression(context, declaration, ClassDescriptor.class);
    }

    @NotNull
    static public NamespaceDescriptor getNamespaceDescriptor(@NotNull BindingContext context,
                                                             @NotNull JetNamespace declaration) {
        return getDescriptorForExpression(context, declaration, NamespaceDescriptor.class);
    }

    @NotNull
    static public FunctionDescriptor getFunctionDescriptor(@NotNull BindingContext context,
                                                           @NotNull JetNamedFunction declaration) {
        return getDescriptorForExpression(context, declaration, FunctionDescriptor.class);
    }

    @NotNull
    static public PropertyAccessorDescriptor getPropertyAccessorDescriptor(@NotNull BindingContext context,
                                                                           @NotNull JetPropertyAccessor declaration) {
        return getDescriptorForExpression(context, declaration, PropertyAccessorDescriptor.class);
    }

    @NotNull
    static public PropertyDescriptor getPropertyDescriptor(@NotNull BindingContext context,
                                                           @NotNull JetProperty declaration) {
        return getDescriptorForExpression(context, declaration, PropertyDescriptor.class);
    }

    @NotNull
    static public String getPropertyNameForPropertyAccessor(@NotNull BindingContext context,
                                                            @NotNull JetPropertyAccessor accessor) {
        PropertyAccessorDescriptor descriptor = getPropertyAccessorDescriptor(context, accessor);
        return descriptor.getCorrespondingProperty().getName();
    }

    @NotNull
    static public PropertySetterDescriptor getPropertySetterDescriptorForProperty(@NotNull BindingContext context,
                                                                                  @NotNull JetProperty property) {
        PropertySetterDescriptor result = getPropertyDescriptor(context, property).getSetter();
        assert result != null : "Property should have a setter descriptor";
        return result;
    }

    @NotNull
    static public PropertyGetterDescriptor getPropertyGetterDescriptorForProperty(@NotNull BindingContext context,
                                                                                  @NotNull JetProperty property) {
        PropertyGetterDescriptor result = getPropertyDescriptor(context, property).getGetter();
        assert result != null : "Property should have a getter descriptor";
        return result;
    }

    @NotNull
    static public List<ClassDescriptor> getSuperclassDescriptors(@NotNull BindingContext context,
                                                                 @NotNull JetClass classDeclaration) {
        ClassDescriptor classDescriptor = getClassDescriptor(context, classDeclaration);
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
    static public ClassDescriptor getClassDescriptorForType(@NotNull JetType type) {
        DeclarationDescriptor superClassDescriptor =
                type.getConstructor().getDeclarationDescriptor();
        assert superClassDescriptor instanceof ClassDescriptor
                : "Superclass descriptor of a type should be of type ClassDescriptor";
        return (ClassDescriptor) superClassDescriptor;
    }

    static public boolean hasAncestorClass(@NotNull BindingContext context, @NotNull JetClass classDeclaration) {
        List<ClassDescriptor> superclassDescriptors = getSuperclassDescriptors(context, classDeclaration);
        return (findAncestorClass(superclassDescriptors) != null);
    }

    static public boolean isStatement(@NotNull BindingContext context, @NotNull JetExpression expression) {
        Boolean isStatement = context.get(BindingContext.STATEMENT, expression);
        assert isStatement != null : "Invalid behaviour of get(BindingContext.STATEMENT)";
        return isStatement;
    }

    @NotNull
    static public JetType getTypeByReference(@NotNull BindingContext context,
                                             @NotNull JetTypeReference typeReference) {
        JetType result = context.get(BindingContext.TYPE, typeReference);
        assert result != null : "TypeReference should reference a type";
        return result;
    }

    @NotNull
    static public ClassDescriptor getClassDescriptorForTypeReference(@NotNull BindingContext context,
                                                                     @NotNull JetTypeReference typeReference) {
        return getClassDescriptorForType(getTypeByReference(context, typeReference));
    }

    //TODO better implementation?
    static private boolean isNotAny(@NotNull DeclarationDescriptor superClassDescriptor) {
        return !superClassDescriptor.getName().equals("Any");
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
}

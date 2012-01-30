package org.jetbrains.k2js.translate.context.declaration;

import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.k2js.translate.context.NamingScope;

import static org.jetbrains.k2js.translate.context.declaration.AnnotationsUtils.annotationStringParameter;
import static org.jetbrains.k2js.translate.context.declaration.AnnotationsUtils.getAnnotationByName;

/**
 * @author Pavel Talanov
 */
public class AnnotatedDeclarationVisitor extends AbstractDeclarationVisitor {

    @NotNull
    private final String classAnnotationFQName;

    @NotNull
    private final String funAnnotationFQName;

    /*package*/ AnnotatedDeclarationVisitor(@NotNull Declarations declarations,
                                            @NotNull String classAnnotationFQName,
                                            @NotNull String funAnnotationFQName) {
        super(declarations);
        this.classAnnotationFQName = classAnnotationFQName;
        this.funAnnotationFQName = funAnnotationFQName;
    }

    @NotNull
    @Override
    protected NamingScope doDeclareScope(@NotNull DeclarationDescriptor descriptor, @NotNull DeclarationContext context,
                                         @NotNull String recommendedName) {
        //TODO: probably need to keep track
        if (!(descriptor instanceof ClassDescriptor)) {
            return context.getScope();
        }
        NamingScope innerScope = context.getScope().innerScope(recommendedName);
        declarations().putScope(descriptor, innerScope);
        return innerScope;
    }

    @NotNull
    @Override
    protected JsName doDeclareName(@NotNull DeclarationDescriptor descriptor, @NotNull DeclarationContext context,
                                   @NotNull String recommendedName) {
        String nativeName = getName(descriptor);
        JsName jsName = context.getScope().
                declareVariable(descriptor, nativeName, false);
        jsName.setObfuscatable(false);
        declarations().putName(descriptor, jsName);
        declarations().putQualifier(descriptor, context.getQualifier());
        return jsName;
    }

    @Override
    protected boolean accept(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof NamespaceDescriptor) {
            return true;
        }
        if (descriptor instanceof FunctionDescriptor) {
            return isAnnotatedFunction((FunctionDescriptor) descriptor);
        }
        if (descriptor instanceof ClassDescriptor) {
            return isAnnotatedClass((ClassDescriptor) descriptor);
        }
        return false;
    }

    @Override
    public void traverseNamespace(@NotNull NamespaceDescriptor namespace, @NotNull DeclarationContext context) {
        declareMembers(namespace, context);
    }


    @NotNull
    public String getName(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof FunctionDescriptor) {
            return getNameForFunction((FunctionDescriptor) descriptor);
        }
        if (descriptor instanceof ClassDescriptor) {
            return getNameForClass((ClassDescriptor) descriptor);
        }
        throw new AssertionError();
    }

    @NotNull
    private String getNameForFunction(@NotNull FunctionDescriptor descriptor) {
        if (hasFunctionAnnotation(descriptor)) {
            return annotationStringParameter(descriptor, funAnnotationFQName);
        }
        if (declaredInAnnotatedClass(descriptor)) {
            return descriptor.getName();
        }
        throw new AssertionError("Use isAnnotatedFunction to check");
    }

    @NotNull
    private String getNameForClass(@NotNull ClassDescriptor descriptor) {
        if (hasClassAnnotation(descriptor)) {
            return descriptor.getName();
        }
        throw new AssertionError("Use isAnnotatedClass to check");
    }

    public boolean isAnnotatedDeclaration(@NotNull DeclarationDescriptor descriptor) {
        return hasClassAnnotation(descriptor) || hasFunctionAnnotation(descriptor)
                || declaredInAnnotatedClass(descriptor);
    }

    public boolean isAnnotatedFunction(@NotNull FunctionDescriptor functionDescriptor) {
        return hasFunctionAnnotation(functionDescriptor) || declaredInAnnotatedClass(functionDescriptor);
    }

    public boolean isAnnotatedClass(@NotNull ClassDescriptor classDescriptor) {
        return hasClassAnnotation(classDescriptor);
    }

    private boolean declaredInAnnotatedClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof ClassDescriptor)) return false;
        return hasClassAnnotation(containingDeclaration);
    }


    private boolean hasFunctionAnnotation(@NotNull DeclarationDescriptor descriptor) {
        return (getAnnotationByName(descriptor, funAnnotationFQName) != null);
    }

    private boolean hasClassAnnotation(@NotNull DeclarationDescriptor descriptor) {
        return (getAnnotationByName(descriptor, classAnnotationFQName) != null);
    }


}

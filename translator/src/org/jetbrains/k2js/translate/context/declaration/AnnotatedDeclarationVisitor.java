package org.jetbrains.k2js.translate.context.declaration;

import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
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
    private final String memberAnnotationFQName;

    /*package*/ AnnotatedDeclarationVisitor(@NotNull Declarations declarations,
                                            @NotNull String classAnnotationFQName,
                                            @NotNull String memberAnnotationFQName) {
        super(declarations);
        this.classAnnotationFQName = classAnnotationFQName;
        this.memberAnnotationFQName = memberAnnotationFQName;
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
        if (descriptor instanceof ClassDescriptor) {
            return isAnnotatedClass((ClassDescriptor) descriptor);
        }

        return isAnnotatedMember(descriptor);
    }

    @Override
    public void traverseNamespace(@NotNull NamespaceDescriptor namespace, @NotNull DeclarationContext context) {
        declareMembers(namespace, context);
    }


    //TODO: refactor
    @NotNull
    public String getName(@NotNull DeclarationDescriptor descriptor) {
        if (hasMemberAnnotation(descriptor)) {
            String name = annotationStringParameter(descriptor, memberAnnotationFQName);
            if (!(name.isEmpty())) {
                return name;
            }
            return descriptor.getName();
        }
        if (declaredInAnnotatedClass(descriptor)) {
            return descriptor.getName();
        }
        if (hasClassAnnotation(descriptor)) {
            return descriptor.getName();
        }
        throw new AssertionError("Use isAnnotatedFunction to check");
    }


    public boolean isAnnotatedMember(@NotNull DeclarationDescriptor descriptor) {
        return hasMemberAnnotation(descriptor) || declaredInAnnotatedClass(descriptor);
    }

    public boolean isAnnotatedClass(@NotNull ClassDescriptor classDescriptor) {
        return hasClassAnnotation(classDescriptor);
    }

    private boolean declaredInAnnotatedClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof ClassDescriptor)) return false;
        return hasClassAnnotation(containingDeclaration);
    }


    private boolean hasMemberAnnotation(@NotNull DeclarationDescriptor descriptor) {
        return (getAnnotationByName(descriptor, memberAnnotationFQName) != null);
    }

    private boolean hasClassAnnotation(@NotNull DeclarationDescriptor descriptor) {
        return (getAnnotationByName(descriptor, classAnnotationFQName) != null);
    }


}

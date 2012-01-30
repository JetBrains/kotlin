package org.jetbrains.k2js.translate.context.declaration;

import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.k2js.translate.context.NamingScope;

import static org.jetbrains.k2js.translate.context.declaration.AnnotationsUtils.doesNotHaveInternalAnnotations;

/**
 * @author Pavel Talanov
 */
public final class KotlinDeclarationVisitor extends AbstractDeclarationVisitor {

    private final boolean shouldObfuscate;

    /*package*/ KotlinDeclarationVisitor(@NotNull Declarations declarations, boolean obfuscateNames) {
        super(declarations);
        this.shouldObfuscate = obfuscateNames;
    }

    @Override
    protected NamingScope doDeclareScope(@NotNull DeclarationDescriptor descriptor, @NotNull DeclarationContext context,
                                         @NotNull String recommendedName) {
        NamingScope innerScope = context.getScope().innerScope(recommendedName);
        declarations().putScope(descriptor, innerScope);
        return innerScope;
    }

    @Override
    @NotNull
    protected JsName doDeclareName(@NotNull DeclarationDescriptor descriptor, @NotNull DeclarationContext context,
                                   @NotNull String recommendedName) {
        JsName jsName = context.getScope().declareVariable(descriptor, recommendedName, shouldObfuscate);
        jsName.setObfuscatable(false);
        declarations().putName(descriptor, jsName);
        declarations().putQualifier(descriptor, context.getQualifier());
        return jsName;
    }

    @Override
    protected boolean accept(@NotNull DeclarationDescriptor descriptor) {
        return (doesNotHaveInternalAnnotations(descriptor));
    }
}

package org.jetbrains.k2js.translate.context.declaration;

import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.k2js.translate.context.NamingScope;

/**
 * @author Pavel Talanov
 */
public final class NativeDeclarationVisitor extends AbstractDeclarationVisitor {

    /*package*/ NativeDeclarationVisitor(@NotNull Declarations nativeDeclarations) {
        super(nativeDeclarations);
    }

    @NotNull
    @Override
    protected NamingScope doDeclareScope(@NotNull DeclarationDescriptor descriptor, @NotNull DeclarationContext context,
                                         @NotNull String recommendedName) {
        //TODO: probably we do
        /*we dont need to declare any scopes for native declarations*/
        return context.getScope();
    }

    @NotNull
    @Override
    protected JsName doDeclareName(@NotNull DeclarationDescriptor descriptor, @NotNull DeclarationContext context,
                                   @NotNull String recommendedName) {
        String nativeName = JsAnnotationsUtils.getNativeName((FunctionDescriptor) descriptor);
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
            return JsAnnotationsUtils.isNativeFunction((FunctionDescriptor) descriptor);
        }
        if (descriptor instanceof ClassDescriptor) {
            return JsAnnotationsUtils.isNativeClass((ClassDescriptor) descriptor);
        }
        throw new AssertionError();
    }


}

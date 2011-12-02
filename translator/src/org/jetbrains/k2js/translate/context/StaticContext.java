package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.backend.js.ast.JsRootScope;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.k2js.declarations.Declarations;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;

public class StaticContext {

    public static StaticContext generateStaticContext(@NotNull JetStandardLibrary library,
                                                      @NotNull BindingContext bindingContext) {
        JsProgram program = new JsProgram("main");
        JsRootScope rootScope = program.getRootScope();
        Namer namer = Namer.newInstance(rootScope);
        Aliaser aliaser = Aliaser.aliasesForStandardClasses(library, namer);
        Declarations declarations = Declarations.newInstance(rootScope);
        Intrinsics intrinsics = Intrinsics.standardLibraryIntrinsics(library);
        return new StaticContext(program, bindingContext, declarations, aliaser, namer, intrinsics);
    }

    @NotNull
    private final JsProgram program;

    @NotNull
    public final BindingContext bindingContext;

    @NotNull
    private final Declarations declarations;

    @NotNull
    private final Aliaser aliaser;

    @NotNull
    private final Namer namer;

    @NotNull
    private final Intrinsics intrinsics;


    private StaticContext(@NotNull JsProgram program, @NotNull BindingContext bindingContext,
                          @NotNull Declarations declarations, @NotNull Aliaser aliaser,
                          @NotNull Namer namer, @NotNull Intrinsics intrinsics) {
        this.program = program;
        this.bindingContext = bindingContext;
        this.declarations = declarations;
        this.aliaser = aliaser;
        this.namer = namer;
        this.intrinsics = intrinsics;
    }

    @NotNull
    public JsProgram getProgram() {
        return program;
    }

    @NotNull
    public BindingContext getBindingContext() {
        return bindingContext;
    }

    @NotNull
    public Declarations getDeclarations() {
        return declarations;
    }

    @NotNull
    public Aliaser getAliaser() {
        return aliaser;
    }

    @NotNull
    public Intrinsics getIntrinsics() {
        return intrinsics;
    }

    @NotNull
    public Namer getNamer() {
        return namer;
    }

    @NotNull
    public JsScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return declarations.getScope(descriptor);
    }

    @NotNull
    public JsScope getScopeForElement(@NotNull JetElement element) {
        DeclarationDescriptor descriptor = getDescriptorForElement(element);
        return getScopeForDescriptor(descriptor);
    }

    @NotNull
    public JsName getNameForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return declarations.getName(descriptor);
    }

    @NotNull
    public JsName getNameForElement(@NotNull JetElement element) {
        DeclarationDescriptor descriptor = getDescriptorForElement(element);
        return getNameForDescriptor(descriptor);
    }

    @NotNull
    DeclarationDescriptor getDescriptorForElement(@NotNull JetElement element) {
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        assert descriptor != null : "Element should have a descriptor";
        return descriptor;
    }

    public boolean isDeclared(@NotNull DeclarationDescriptor descriptor) {
        return declarations.hasDeclaredName(descriptor);
    }
}
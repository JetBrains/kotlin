package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.backend.js.ast.JsRootScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;
import org.jetbrains.k2js.translate.utils.BindingUtils;

public class StaticContext {

    public static StaticContext generateStaticContext(@NotNull JetStandardLibrary library,
                                                      @NotNull BindingContext bindingContext) {
        JsProgram program = new JsProgram("main");
        JsRootScope jsRootScope = program.getRootScope();
        Namer namer = Namer.newInstance(jsRootScope);
        Aliaser aliaser = Aliaser.newInstance();
        NamingScope scope = NamingScope.rootScope(jsRootScope);
        Declarations declarations = Declarations.newInstance(scope);
        Intrinsics intrinsics = Intrinsics.standardLibraryIntrinsics(library);
        StandardClasses standardClasses = StandardClasses.bindImplementations(library, namer.getKotlinScope());
        return new StaticContext(program, bindingContext, declarations, aliaser,
                namer, intrinsics, standardClasses, scope);
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

    @NotNull
    private final StandardClasses standardClasses;

    @NotNull
    private final NamingScope rootScope;


    private StaticContext(@NotNull JsProgram program, @NotNull BindingContext bindingContext,
                          @NotNull Declarations declarations, @NotNull Aliaser aliaser,
                          @NotNull Namer namer, @NotNull Intrinsics intrinsics,
                          @NotNull StandardClasses standardClasses, @NotNull NamingScope rootScope) {
        this.program = program;
        this.bindingContext = bindingContext;
        this.declarations = declarations;
        this.aliaser = aliaser;
        this.namer = namer;
        this.intrinsics = intrinsics;
        this.rootScope = rootScope;
        this.standardClasses = standardClasses;
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
    public NamingScope getRootScope() {
        return rootScope;
    }

    @NotNull
    public StandardClasses getStandardClasses() {
        return standardClasses;
    }

    @NotNull
    public NamingScope getScopeForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        return declarations.getScope(descriptor);
    }

    @NotNull
    public NamingScope getScopeForElement(@NotNull JetElement element) {
        DeclarationDescriptor descriptor = BindingUtils.getDescriptorForElement(bindingContext, element);
        return getScopeForDescriptor(descriptor);
    }

    @NotNull
    public JsName getGlobalName(@NotNull DeclarationDescriptor descriptor) {
        JsName nameToDeclare = declarations.getName(descriptor);
        nameToDeclare.setObfuscatable(false);
        return nameToDeclare;
    }

    public boolean isDeclared(@NotNull DeclarationDescriptor descriptor) {
        return declarations.hasDeclaredName(descriptor);
    }
}
package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.backend.js.ast.JsRootScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.k2js.translate.context.declaration.DeclarationFacade;
import org.jetbrains.k2js.translate.context.declaration.Declarations;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;

public class StaticContext {

    public static StaticContext generateStaticContext(@NotNull JetStandardLibrary library,
                                                      @NotNull BindingContext bindingContext) {
        JsProgram program = new JsProgram("main");
        JsRootScope jsRootScope = program.getRootScope();
        Namer namer = Namer.newInstance(jsRootScope);
        Aliaser aliaser = Aliaser.newInstance();
        NamingScope scope = NamingScope.rootScope(jsRootScope);
        DeclarationFacade declarations = DeclarationFacade.createFacade(scope);
        Intrinsics intrinsics = Intrinsics.standardLibraryIntrinsics(library);
        StandardClasses standardClasses =
                StandardClasses.bindImplementations(namer.getKotlinScope());
        return new StaticContext(program, bindingContext, declarations, aliaser,
                namer, intrinsics, standardClasses, scope);
    }

    @NotNull
    private final JsProgram program;

    @NotNull
    private final BindingContext bindingContext;

    @NotNull
    private final DeclarationFacade declarationFacade;

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


    //TODO: too many parameters in constructor
    private StaticContext(@NotNull JsProgram program, @NotNull BindingContext bindingContext,
                          @NotNull DeclarationFacade declarations, @NotNull Aliaser aliaser,
                          @NotNull Namer namer, @NotNull Intrinsics intrinsics,
                          @NotNull StandardClasses standardClasses, @NotNull NamingScope rootScope) {
        this.program = program;
        this.bindingContext = bindingContext;
        this.declarationFacade = declarations;
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
    public DeclarationFacade getDeclarationFacade() {
        return declarationFacade;
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

    //TODO: consider using nullable return value instead
    @NotNull
    public JsName getGlobalName(@NotNull DeclarationDescriptor descriptor) {
        for (Declarations declarations : declarationFacade.getAllDeclarations()) {
            if (declarations.hasDeclaredName(descriptor)) {
                return declarations.getName(descriptor);
            }
        }
        throw new AssertionError("Use is declared method to check.");
    }

    public boolean isDeclared(@NotNull DeclarationDescriptor descriptor) {
        for (Declarations declarations : declarationFacade.getAllDeclarations()) {
            if (declarations.hasDeclaredName(descriptor)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public JsNameRef getQualifier(@NotNull DeclarationDescriptor descriptor) {
        for (Declarations declarations : declarationFacade.getAllDeclarations()) {
            if (declarations.hasQualifier(descriptor)) {
                return declarations.getQualifier(descriptor);
            }
        }
        throw new AssertionError("Use hasQualifier method to check.");
    }

    public boolean hasQualifier(@NotNull DeclarationDescriptor descriptor) {
        for (Declarations declarations : declarationFacade.getAllDeclarations()) {
            if (declarations.hasQualifier(descriptor)) {
                return true;
            }
        }
        return false;
    }
}
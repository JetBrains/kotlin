package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.backend.js.ast.JsRootScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.k2js.translate.intrinsic.Intrinsics;

public class StaticContext {

    public static StaticContext generateStaticContext(@NotNull JetStandardLibrary library,
                                                      @NotNull BindingContext bindingContext) {
        JsProgram program = new JsProgram("main");
        JsRootScope jsRootScope = program.getRootScope();
        Namer namer = Namer.newInstance(jsRootScope);
        Aliaser aliaser = Aliaser.newInstance();
        NamingScope scope = NamingScope.rootScope(jsRootScope);
        Intrinsics intrinsics = Intrinsics.standardLibraryIntrinsics(library);
        StandardClasses standardClasses =
                StandardClasses.bindImplementations(namer.getKotlinScope());
        return new StaticContext(program, bindingContext, aliaser,
                namer, intrinsics, standardClasses, scope);
    }

    @NotNull
    private final JsProgram program;

    @NotNull
    private final BindingContext bindingContext;

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
                          @NotNull Aliaser aliaser,
                          @NotNull Namer namer, @NotNull Intrinsics intrinsics,
                          @NotNull StandardClasses standardClasses, @NotNull NamingScope rootScope) {
        this.program = program;
        this.bindingContext = bindingContext;
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
}
package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import com.sun.xml.internal.ws.wsdl.writer.document.Binding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingContext;

/**
 * @author Talanov Pavel
 */
public class TranslationContext {

    private final JsBlock currentBlock;
    private final JsNameRef currentNamespace;
    private final JsProgram program;
    private final BindingContext context;
    private final JsScope enclosingScope;
    private final ContextType type;


    private TranslationContext(JsNameRef namespace, JsBlock block, JsProgram program,
                               BindingContext bindingContext, JsScope enclosingScope, ContextType type) {
        assert block != null;
        assert program != null;
        assert bindingContext != null;
        assert enclosingScope != null;
        assert type != null;
        this.currentBlock = block;
        this.currentNamespace = namespace;
        this.program = program;
        this.context = bindingContext;
        this.enclosingScope = enclosingScope;
        this.type = type;
    }

    @NotNull
    public static TranslationContext rootContext(JsProgram program, BindingContext bindingContext) {
        return new TranslationContext(null, program.getFragmentBlock(0),
                program, bindingContext, program.getRootScope(), ContextType.NAMESPACE_BODY);
    }

    //TODO implement correct factories
    @NotNull
    public TranslationContext newNamespace(JsNameRef newNamespace) {
        return new TranslationContext(newNamespace, currentBlock, program,
                context, enclosingScope, type);
    }

    @NotNull
    public TranslationContext newBlock(JsBlock newBlock) {
        return new TranslationContext(currentNamespace, newBlock, program,
                context, enclosingScope, type);
    }

    @NotNull
    public TranslationContext newScope(JsScope newScope) {
        return new TranslationContext(currentNamespace, currentBlock, program,
                context, newScope, type);
    }

    @NotNull
    public TranslationContext newType(ContextType newType) {
        return new TranslationContext(currentNamespace, currentBlock, program,
                context, enclosingScope, newType);
    }

    @NotNull
    public JsNameRef getQualifiedReference(JsName name) {
        if (currentNamespace != null) {
            return AstUtil.newNameRef(currentNamespace, name);
        }
        return new JsNameRef(name);
    }

    @NotNull
    public JsName getJSName(String jetName) {
        //TODO dummy implemetation
        return new JsName(program().getScope(), jetName, jetName, jetName);
    }

    @NotNull
    public BindingContext bindingContext() {
        return context;
    }

    @NotNull
    public JsProgram program() {
        return program;
    }

    @NotNull
    JsScope scope() {
        return enclosingScope;
    }

    @NotNull
    JsBlock block() {
        return currentBlock;
    }

    @NotNull
    ContextType type() {
        return type;
    }


}

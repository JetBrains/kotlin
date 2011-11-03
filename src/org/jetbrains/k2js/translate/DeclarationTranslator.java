package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author Talanov Pavel
 */
public class DeclarationTranslator extends AbstractTranslator {

    private final State state;

    public DeclarationTranslator(TranslationContext context) {
        super(context);
        state = context.type().getDeclarationTranslatorState(this);
    }

    @NotNull
    JsStatement translateDeclaration(JetDeclaration declaration) {
        if (declaration instanceof JetProperty) {
            return (translateProperty((JetProperty) declaration));
        }
        if (declaration instanceof JetNamedFunction) {
            return AstUtil.convertToStatement((new FunctionTranslator(translationContext()))
                    .translateFunction((JetNamedFunction) declaration));
        }
        else if (declaration instanceof JetClass) {
            ClassTranslator classTranslator = new ClassTranslator(translationContext());
            return classTranslator.translateClass((JetClass)declaration);
        }
        else if (declaration instanceof JetNamespace) {
           // JetNamespace childNamespace = (JetNamespace) declaration;
           // state.forNamespace(childNamespace).translate(childNamespace);
        }
        throw new RuntimeException("Unexpected declaration " + declaration.toString());
    }

    @NotNull
    public JsStatement translateProperty(JetProperty declaration) {
        return state.translateProperty(declaration);
    }

    @Nullable
    private JsExpression translateInitializer(JetProperty declaration) {
        JsExpression jsInitExpression = null;
        JetExpression initializer = declaration.getInitializer();
        if (initializer != null) {
            // TODO hack alert
            jsInitExpression = (JsExpression)
                (new ExpressionTranslator(translationContext())).translate(initializer);
        }
        return jsInitExpression;
    }

    public abstract class State {
        public abstract JsStatement translateProperty(JetProperty declaration);
   }

    public final class FunctionVariableDeclaration extends State {
        @NotNull
        public JsStatement translateProperty(JetProperty declaration) {
            String propertyName = declaration.getName();
            JsName jsPropertyName = getJSName(propertyName);
            JsExpression jsInitExpression = translateInitializer(declaration);
            return AstUtil.newVar(jsPropertyName, jsInitExpression);
        }

    }

    public final class NamespacePropertyDeclaration extends State {
        @NotNull
        public JsStatement translateProperty(JetProperty declaration) {
            String propertyName = declaration.getName();
            JsNameRef jsPropertyNameReference = translationContext().getNamespaceQualifiedReference(getJSName(propertyName));
            JsExpression jsInitExpression = translateInitializer(declaration);
            JsExpression result;
            if (jsInitExpression != null) {
                result = AstUtil.newAssignment(jsPropertyNameReference, jsInitExpression);
            }
            else {
                result = jsPropertyNameReference;
            }
            return AstUtil.convertToStatement(result);
        }
    }



}

package org.jetbrains.k2js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetDelegatorToSuperCall;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.*;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.assignmentToBackingField;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.functionWithScope;

/**
 * @author Pavel Talanov
 */
public final class ClassInitializerTranslator extends AbstractInitializerTranslator {

    @NotNull
    private final JetClass classDeclaration;
    @NotNull
    private final List<JsStatement> initializerStatements = new ArrayList<JsStatement>();

    public ClassInitializerTranslator(@NotNull JetClass classDeclaration, @NotNull TranslationContext context) {
        super(context.getScopeForElement(classDeclaration).innerScope
                ("initializer " + classDeclaration.getName()), context);
        this.classDeclaration = classDeclaration;
    }

    @Override
    @NotNull
    protected JsFunction generateInitializerFunction() {
        JsFunction result = functionWithScope(initializerMethodScope);
        //NOTE: that while we translateAsLocalNameReference constructor parameters we also add property initializer statements
        // for properties declared as constructor parameters
        result.setParameters(translatePrimaryConstructorParameters());
        mayBeAddCallToSuperMethod();
        result.setBody(generateInitializerMethodBody());
        return result;
    }

    @NotNull
    private JsBlock generateInitializerMethodBody() {
        initializerStatements.addAll(translateClassInitializers(classDeclaration));
        return AstUtil.newBlock(initializerStatements);
    }

    private void mayBeAddCallToSuperMethod() {
        if (hasAncestorClass(context().bindingContext(), classDeclaration)) {
            JetDelegatorToSuperCall superCall = getSuperCall();
            if (superCall == null) return;
            addCallToSuperMethod(superCall);
        }
    }

    private void addCallToSuperMethod(@NotNull JetDelegatorToSuperCall superCall) {
        //TODO: look into
        JsName superMethodName = initializerMethodScope.jsScope().declareName(Namer.superMethodName());
        List<JsExpression> arguments = translateArguments(superCall);
        initializerStatements.add(AstUtil.convertToStatement
                (AstUtil.newInvocation(AstUtil.thisQualifiedReference(superMethodName), arguments)));
    }

    @NotNull
    private List<JsExpression> translateArguments(@NotNull JetDelegatorToSuperCall superCall) {
        //TODO: use the same mechanism as in call translator
        return TranslationUtils.translateArgumentList(context(), superCall.getValueArguments());
    }

    @Nullable
    private JetDelegatorToSuperCall getSuperCall() {
        JetDelegatorToSuperCall result = null;
        for (JetDelegationSpecifier specifier : classDeclaration.getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorToSuperCall) {
                result = (JetDelegatorToSuperCall) specifier;
            }
        }
        //assert result != null : "Class must call ancestor's constructor.";
        return result;
    }

    @NotNull
    List<JsParameter> translatePrimaryConstructorParameters() {
        List<JetParameter> parameterList = classDeclaration.getPrimaryConstructorParameters();
        List<JsParameter> result = new ArrayList<JsParameter>();
        for (JetParameter jetParameter : parameterList) {
            result.add(translateParameter(jetParameter));
        }
        return result;
    }

    @NotNull
    private JsParameter translateParameter(@NotNull JetParameter jetParameter) {
        DeclarationDescriptor parameterDescriptor =
                getDescriptorForElement(context().bindingContext(), jetParameter);
        JsName parameterName = context().getNameForDescriptor(parameterDescriptor);
        JsParameter jsParameter = new JsParameter(parameterName);
        mayBeAddInitializerStatementForProperty(jsParameter, jetParameter);
        return jsParameter;
    }

    private void mayBeAddInitializerStatementForProperty(@NotNull JsParameter jsParameter,
                                                         @NotNull JetParameter jetParameter) {
        PropertyDescriptor propertyDescriptor =
                getPropertyDescriptorForConstructorParameter(context().bindingContext(), jetParameter);
        if (propertyDescriptor != null) {
            JsStatement assignmentToBackingFieldExpression = assignmentToBackingField
                    (context(), propertyDescriptor, jsParameter.getName().makeRef());
            initializerStatements.add(assignmentToBackingFieldExpression);
        }
    }


}

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.declaration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtParameter;

import java.util.List;

public class JsInlineClassGenerator extends JsEqualsHashcodeToStringGenerator {

    protected JsInlineClassGenerator(KtClassOrObject klass, TranslationContext context) {
        super(klass, context);
    }

    @Override
    public void generate() {
        generateUnboxFunction();

        super.generate();
    }

    private void generateUnboxFunction() {
        PropertyDescriptor boxee = getPrimaryConstructorProperties().get(0);

        JsFunction unboxFunction = context.createRootScopedFunction("unbox");
        JsExpression prototypeRef = JsAstUtils.prototypeOf(context.getInnerReference(getClassDescriptor()));
        JsExpression functionRef = new JsNameRef("unbox", prototypeRef);

        unboxFunction.getBody().getStatements().add(new JsReturn(JsAstUtils.pureFqn(context.getNameForDescriptor(boxee), new JsThisRef())));

        context.addDeclarationStatement(JsAstUtils.assignment(functionRef, unboxFunction).makeStmt());
    }
}

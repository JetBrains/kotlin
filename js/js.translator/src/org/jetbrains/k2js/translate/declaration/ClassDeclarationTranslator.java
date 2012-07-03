/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import gnu.trove.THashMap;
import gnu.trove.TLinkable;
import gnu.trove.TLinkableAdaptor;
import gnu.trove.TLinkedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.ArrayList;
import java.util.List;

import static com.google.dart.compiler.backend.js.ast.JsVars.JsVar;
import static org.jetbrains.k2js.translate.general.Translation.translateClassDeclaration;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getClassDescriptor;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.newBlock;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Generates a big block where are all the classes(objects representing them) are created.
 */
public final class ClassDeclarationTranslator extends AbstractTranslator {
    private int localNameCounter;

    @NotNull
    private final THashMap<JetClass, ListItem> openClassToItem = new THashMap<JetClass, ListItem>();

    private final TLinkedList<ListItem> openList = new TLinkedList<ListItem>();
    private final List<ListItem> finalList = new ArrayList<ListItem>();

    @NotNull
    private final JsFunction dummyFunction;

    private final JsName declarationsObject;
    private final JsVar classesVar;

    public ClassDeclarationTranslator(@NotNull TranslationContext context) {
        super(context);

        dummyFunction = new JsFunction(context.jsScope());
        declarationsObject = context().jsScope().declareName(Namer.nameForClassesVariable());
        classesVar = new JsVars.JsVar(declarationsObject);
    }

    private final class OpenClassRefProvider implements ClassAliasingMap {
        @Override
        @Nullable
        public JsNameRef get(JetClass declaration, JetClass referencedDeclaration) {
            ListItem item = openClassToItem.get(declaration);
            // class declared in library
            if (item == null) {
                return null;
            }

            addAfter(item, openClassToItem.get(referencedDeclaration));
            return item.label;
        }

        private void addAfter(@NotNull ListItem item, @NotNull ListItem referencedItem) {
            for (TLinkable link = item.getNext(); link != null; link = link.getNext()) {
                if (link == referencedItem) {
                    return;
                }
            }

            openList.remove(referencedItem);
            openList.addBefore((ListItem) item.getNext(), referencedItem);
        }
    }

    private final class FinalClassRefProvider implements ClassAliasingMap {
        @Override
        public JsNameRef get(JetClass declaration, JetClass referencedDeclaration) {
            ListItem item = openClassToItem.get(declaration);
            return item == null ? null : item.label;
        }
    }

    private static class ListItem extends TLinkableAdaptor {
        private final JetClass declaration;
        private final JsNameRef label;

        private JsExpression translatedDeclaration;

        private ListItem(JetClass declaration, JsNameRef label) {
            this.declaration = declaration;
            this.label = label;
        }
    }

    @NotNull
    public JsVars getDeclarationsStatement() {
        JsVars vars = new JsVars();
        vars.add(classesVar);
        return vars;
    }

    public void generateDeclarations() {
        JsObjectLiteral valueLiteral = new JsObjectLiteral();
        JsVars vars = new JsVars();
        List<JsPropertyInitializer> propertyInitializers = valueLiteral.getPropertyInitializers();

        generateOpenClassDeclarations(vars, propertyInitializers);
        generateFinalClassDeclarations(vars, propertyInitializers);

        if (vars.isEmpty()) {
            classesVar.setInitExpr(valueLiteral);
            return;
        }

        List<JsStatement> result = new ArrayList<JsStatement>();
        result.add(vars);
        result.add(new JsReturn(valueLiteral));
        dummyFunction.setBody(newBlock(result));
        classesVar.setInitExpr(AstUtil.newInvocation(dummyFunction));
    }

    private void generateOpenClassDeclarations(@NotNull JsVars vars, @NotNull List<JsPropertyInitializer> propertyInitializers) {
        ClassAliasingMap classAliasingMap = new OpenClassRefProvider();
        // first pass: set up list order
        for (ListItem item : openList) {
            item.translatedDeclaration = translateClassDeclaration(item.declaration, classAliasingMap, context());
        }
        // second pass: generate
        for (ListItem item : openList) {
            generate(item, propertyInitializers, item.translatedDeclaration, vars);
        }
    }

    private void generateFinalClassDeclarations(@NotNull JsVars vars, @NotNull List<JsPropertyInitializer> propertyInitializers) {
        ClassAliasingMap classAliasingMap = new FinalClassRefProvider();
        for (ListItem item : finalList) {
            generate(item, propertyInitializers, translateClassDeclaration(item.declaration, classAliasingMap, context()), vars);
        }
    }

    private static void generate(@NotNull ListItem item,
            @NotNull List<JsPropertyInitializer> propertyInitializers,
            @NotNull JsExpression definition,
            @NotNull JsVars vars) {
        JsExpression value;
        if (item.label.getName() == null) {
            value = definition;
        }
        else {
            assert item.label.getName() != null;
            vars.add(new JsVar(item.label.getName(), definition));
            value = item.label;
        }

        propertyInitializers.add(new JsPropertyInitializer(item.label, value));
    }

    @NotNull
    public JsPropertyInitializer translateAndGetClassNameToClassObject(@NotNull JetClass declaration) {
        ClassDescriptor descriptor = getClassDescriptor(context().bindingContext(), declaration);

        JsNameRef labelRef;
        String label = 'c' + Integer.toString(localNameCounter++, 36);
        boolean isFinal = descriptor.getModality() == Modality.FINAL;
        if (isFinal) {
            labelRef = new JsNameRef(label);
        }
        else {
            labelRef = dummyFunction.getScope().declareName(label).makeRef();
        }

        ListItem item = new ListItem(declaration, labelRef);
        if (isFinal) {
            finalList.add(item);
        }
        else {
            openList.add(item);
            openClassToItem.put(declaration, item);
        }

        JsNameRef qualifiedLabelRef = new JsNameRef(labelRef.getIdent());
        qualifiedLabelRef.setQualifier(declarationsObject.makeRef());
        JsExpression value;
        if (context().isEcma5()) {
            value = JsAstUtils.createDataDescriptor(qualifiedLabelRef, false, context());
        }
        else {
            value = qualifiedLabelRef;
        }

        return new JsPropertyInitializer(context().program().getStringLiteral(descriptor.getName().getName()), value);
    }
}

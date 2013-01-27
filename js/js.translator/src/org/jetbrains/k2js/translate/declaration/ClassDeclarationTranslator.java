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
import com.intellij.openapi.util.Pair;
import com.intellij.util.SmartList;
import gnu.trove.THashMap;
import gnu.trove.TLinkable;
import gnu.trove.TLinkableAdaptor;
import gnu.trove.TLinkedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.k2js.translate.LabelGenerator;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.initializer.InitializerUtils;

import java.util.ArrayList;
import java.util.List;

import static com.google.dart.compiler.backend.js.ast.JsVars.JsVar;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getClassDescriptor;
import static org.jetbrains.k2js.translate.utils.ErrorReportingUtils.message;

/**
 * Generates a big block where are all the classes(objects representing them) are created.
 */
public final class ClassDeclarationTranslator extends AbstractTranslator {
    private final LabelGenerator localLabelGenerator = new LabelGenerator('c');

    @NotNull
    private final THashMap<ClassDescriptor, FinalListItem> openClassDescriptorToItem = new THashMap<ClassDescriptor, FinalListItem>();

    private final TLinkedList<FinalListItem> openList = new TLinkedList<FinalListItem>();
    private final List<Pair<JetClassOrObject, JsInvocation>> finalList = new ArrayList<Pair<JetClassOrObject, JsInvocation>>();

    @NotNull
    private final JsFunction dummyFunction;

    private final JsNameRef declarationsObjectRef;
    private final JsVar classesVar;

    public ClassDeclarationTranslator(@NotNull TranslationContext context) {
        super(context);

        dummyFunction = new JsFunction(context.scope());
        JsName declarationsObject = context().scope().declareName(Namer.nameForClassesVariable());
        classesVar = new JsVars.JsVar(declarationsObject);
        declarationsObjectRef = declarationsObject.makeRef();
    }

    private final class OpenClassRefProvider implements ClassAliasingMap {
        @Override
        @Nullable
        public JsNameRef get(ClassDescriptor descriptor, ClassDescriptor referencedDescriptor) {
            FinalListItem item = openClassDescriptorToItem.get(descriptor);
            // class declared in library
            if (item == null) {
                return null;
            }

            addAfter(item, openClassDescriptorToItem.get(referencedDescriptor));
            return item.label;
        }

        private void addAfter(@NotNull FinalListItem item, @NotNull FinalListItem referencedItem) {
            for (TLinkable link = item.getNext(); link != null; link = link.getNext()) {
                if (link == referencedItem) {
                    return;
                }
            }

            openList.remove(referencedItem);
            openList.addBefore((FinalListItem) item.getNext(), referencedItem);
        }
    }

    private static class FinalListItem extends TLinkableAdaptor {
        private final ClassDescriptor descriptor;
        private final JetClass declaration;
        private final JsNameRef label;
        private final JsNameRef qualifiedLabel;

        private JsExpression translatedDeclaration;

        private FinalListItem(JetClass declaration, ClassDescriptor descriptor, JsNameRef label, JsNameRef qualifiedLabel) {
            this.descriptor = descriptor;
            this.declaration = declaration;
            this.label = label;
            this.qualifiedLabel = qualifiedLabel;
        }
    }

    @NotNull
    public JsVars.JsVar getDeclaration() {
        return classesVar;
    }

    public void generateDeclarations() {
        List<JsVar> vars = new SmartList<JsVar>();
        List<JsPropertyInitializer> propertyInitializers = new SmartList<JsPropertyInitializer>();

        generateOpenClassDeclarations(vars, propertyInitializers);
        generateFinalClassDeclarations();

        if (vars.isEmpty()) {
            if (!propertyInitializers.isEmpty()) {
                classesVar.setInitExpression(new JsObjectLiteral(propertyInitializers));
            }
            return;
        }

        dummyFunction.setBody(new JsBlock(new JsVars(vars, true), new JsReturn(new JsObjectLiteral(propertyInitializers))));
        classesVar.setInitExpression(new JsInvocation(dummyFunction));
    }

    private void generateOpenClassDeclarations(@NotNull List<JsVar> vars, @NotNull List<JsPropertyInitializer> propertyInitializers) {
        ClassAliasingMap classAliasingMap = new OpenClassRefProvider();
        // first pass: set up list order
        for (FinalListItem item : openList) {
            item.translatedDeclaration =
                    new ClassTranslator(item.declaration, item.descriptor, classAliasingMap, context()).translate(context());
        }
        // second pass: generate
        for (FinalListItem item : openList) {
            JsExpression translatedDeclaration = item.translatedDeclaration;
            if (translatedDeclaration == null) {
                throw new IllegalStateException(message(item.declaration, "Could not translate class declaration)"));
            }
            generate(item, propertyInitializers, translatedDeclaration, vars);
        }
    }

    private void generateFinalClassDeclarations() {
        ClassAliasingMap aliasingMap = new ClassAliasingMap() {
            @Override
            public JsNameRef get(ClassDescriptor descriptor, ClassDescriptor referencedDescriptor) {
                FinalListItem item = openClassDescriptorToItem.get(descriptor);
                return item == null ? null : item.qualifiedLabel;
            }
        };

        for (Pair<JetClassOrObject, JsInvocation> item : finalList) {
            new ClassTranslator(item.first, aliasingMap, context()).translate(item.second);
        }
    }

    private static void generate(@NotNull FinalListItem item,
            @NotNull List<JsPropertyInitializer> propertyInitializers,
            @NotNull JsExpression definition,
            @NotNull List<JsVar> vars) {
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
    public JsPropertyInitializer translate(@NotNull JetClassOrObject declaration) {
        ClassDescriptor descriptor = getClassDescriptor(context().bindingContext(), declaration);
        JsExpression value;
        if (descriptor.getModality() == Modality.FINAL) {
            JsInvocation invocation = context().namer().classCreateInvocation(descriptor);
            finalList.add(new Pair<JetClassOrObject, JsInvocation>(declaration, invocation));
            value = invocation;
        }
        else {
            String label = localLabelGenerator.generate();
            JsNameRef labelRef = dummyFunction.getScope().declareName(label).makeRef();
            FinalListItem item = new FinalListItem((JetClass) declaration, descriptor, labelRef, new JsNameRef(labelRef.getIdent(), declarationsObjectRef));
            openList.add(item);
            openClassDescriptorToItem.put(descriptor, item);

            value = item.qualifiedLabel;
        }

        return InitializerUtils.createPropertyInitializer(descriptor, value, context());
    }
}

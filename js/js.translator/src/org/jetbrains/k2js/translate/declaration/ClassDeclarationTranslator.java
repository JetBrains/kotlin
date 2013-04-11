/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.utils.DFS;
import org.jetbrains.k2js.translate.LabelGenerator;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.initializer.InitializerUtils;

import java.util.*;

import static com.google.dart.compiler.backend.js.ast.JsVars.JsVar;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassDescriptorForType;
import static org.jetbrains.k2js.translate.general.Translation.translateClassDeclaration;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getClassDescriptor;

/**
 * Generates a big block where are all the classes(objects representing them) are created.
 */
public final class ClassDeclarationTranslator extends AbstractTranslator {
    private final LabelGenerator localLabelGenerator = new LabelGenerator('c');

    @NotNull
    private final THashMap<ClassDescriptor, OpenClassInfo> openClassDescriptorToItem = new THashMap<ClassDescriptor, OpenClassInfo>();

    private final LinkedList<OpenClassInfo> openList = new LinkedList<OpenClassInfo>();
    private final List<Pair<JetClassOrObject, JsInvocation>> finalList = new ArrayList<Pair<JetClassOrObject, JsInvocation>>();

    @NotNull
    private final ClassDescriptorToLabel classDescriptorToLabel = new ClassDescriptorToLabel();

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

    private final class ClassDescriptorToLabel implements ClassAliasingMap {
        @Override
        @Nullable
        public JsNameRef get(ClassDescriptor descriptor, ClassDescriptor referencedDescriptor) {
            OpenClassInfo item = openClassDescriptorToItem.get(descriptor);
            // class declared in library
            if (item == null) {
                return null;
            }

            return item.label;
        }
    }

    private static class OpenClassInfo {
        private final JetClass declaration;
        private final JsNameRef label;
        private final JsNameRef qualifiedLabel;

        private OpenClassInfo(JetClass declaration, JsNameRef label, JsNameRef qualifiedLabel) {
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
        // first pass: set up list order
        LinkedList<OpenClassInfo> sortedOpenClasses =
                (LinkedList<OpenClassInfo>) DFS.topologicalOrder(openList, new DFS.Neighbors<OpenClassInfo>() {
                    @NotNull
                    @Override
                    public Iterable<OpenClassInfo> getNeighbors(OpenClassInfo current) {
                        LinkedList<OpenClassInfo> parents = new LinkedList<OpenClassInfo>();
                        ClassDescriptor classDescriptor = getClassDescriptor(context().bindingContext(), current.declaration);
                        Collection<JetType> superTypes = classDescriptor.getTypeConstructor().getSupertypes();

                        for (JetType type : superTypes) {
                            ClassDescriptor descriptor = getClassDescriptorForType(type);
                            OpenClassInfo item = openClassDescriptorToItem.get(descriptor);
                            if (item == null) {
                                continue;
                            }

                            parents.add(item);
                        }

                        return parents;
                    }
                });

        assert sortedOpenClasses.size() == openList.size();

        // second pass: generate
        Iterator<OpenClassInfo> it = sortedOpenClasses.descendingIterator();
        while (it.hasNext()) {
            OpenClassInfo item = it.next();
            JsExpression translatedDeclaration = translateClassDeclaration(item.declaration, classDescriptorToLabel, context());
            generate(item, propertyInitializers, translatedDeclaration, vars);
        }
    }

    private void generateFinalClassDeclarations() {
        ClassAliasingMap aliasingMap = new ClassAliasingMap() {
            @Override
            public JsNameRef get(ClassDescriptor descriptor, ClassDescriptor referencedDescriptor) {
                OpenClassInfo item = openClassDescriptorToItem.get(descriptor);
                return item == null ? null : item.qualifiedLabel;
            }
        };

        for (Pair<JetClassOrObject, JsInvocation> item : finalList) {
            new ClassTranslator(item.first, aliasingMap, context()).translateClassOrObjectCreation(item.second);
        }
    }

    private static void generate(@NotNull OpenClassInfo item,
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
            OpenClassInfo
                    item = new OpenClassInfo((JetClass) declaration, labelRef, new JsNameRef(labelRef.getIdent(), declarationsObjectRef));
            openList.add(item);
            openClassDescriptorToItem.put(descriptor, item);

            value = item.qualifiedLabel;
        }

        return InitializerUtils.createPropertyInitializer(descriptor, value, context());
    }
}

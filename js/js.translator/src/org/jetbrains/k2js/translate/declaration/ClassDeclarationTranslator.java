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
import com.intellij.util.SmartList;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectObjectProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.utils.DFS;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.google.dart.compiler.backend.js.ast.JsVars.JsVar;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassDescriptorForType;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getClassDescriptor;

/**
 * Generates a big block where are all the classes(objects representing them) are created.
 */
public final class ClassDeclarationTranslator extends AbstractTranslator {
    private final THashSet<String> nameClashGuard = new THashSet<String>();

    @NotNull
    private final THashMap<ClassDescriptor, OpenClassInfo> openClassDescriptorToItem = new THashMap<ClassDescriptor, OpenClassInfo>();

    private final LinkedList<OpenClassInfo> openList = new LinkedList<OpenClassInfo>();
    @NotNull
    private final ClassDescriptorToLabel classDescriptorToLabel = new ClassDescriptorToLabel();

    private final THashMap<ClassDescriptor, JsNameRef> openClassDescriptorToQualifiedLabel = new THashMap<ClassDescriptor, JsNameRef>();

    private final ClassAliasingMap classDescriptorToQualifiedLabel = new ClassAliasingMap() {
        @NotNull
        @Override
        public JsNameRef get(ClassDescriptor descriptor, ClassDescriptor referencedDescriptor) {
            JsNameRef ref = openClassDescriptorToQualifiedLabel.get(descriptor);
            if (ref != null) {
                return ref;
            }

            // will be resolved later
            ref = new JsNameRef("<unresolved class>");
            openClassDescriptorToQualifiedLabel.put(descriptor, ref);
            return ref;
        }
    };

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
        @Nullable
        @Override
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
        private final ClassDescriptor descriptor;
        private final JetClass declaration;
        private final JsNameRef label;
        private boolean referencedFromOpenClass = false;

        private OpenClassInfo(JetClass declaration, ClassDescriptor descriptor, JsNameRef label) {
            this.descriptor = descriptor;
            this.declaration = declaration;
            this.label = label;
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
        fixUnresolvedClassReferences();

        if (vars.isEmpty()) {
            if (!propertyInitializers.isEmpty()) {
                classesVar.setInitExpression(new JsObjectLiteral(propertyInitializers, true));
            }
            return;
        }
        if (!vars.isEmpty() || !propertyInitializers.isEmpty()) {
            throw new IllegalStateException();  // check, that all class generate as final class
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

                            item.referencedFromOpenClass = true;
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
            JsExpression translatedDeclaration =
                    new ClassTranslator(item.declaration, item.descriptor, classDescriptorToLabel, context()).translate();

            JsExpression value;
            if (item.referencedFromOpenClass) {
                vars.add(new JsVar(item.label.getName(), translatedDeclaration));
                value = item.label;
            }
            else {
                value = translatedDeclaration;
            }

            propertyInitializers.add(new JsPropertyInitializer(item.label, value));
        }
    }

    private void fixUnresolvedClassReferences() {
        openClassDescriptorToQualifiedLabel.forEachEntry(new TObjectObjectProcedure<ClassDescriptor, JsNameRef>() {
            @Override
            public boolean execute(ClassDescriptor descriptor, JsNameRef ref) {
                if (ref.getName() == null) {
                    // from library
                    ref.resolve(context().getNameForDescriptor(descriptor));
                    ref.setQualifier(context().getQualifierForDescriptor(descriptor));
                }
                return true;
            }
        });
    }

    private String createNameForClass(ClassDescriptor descriptor) {
        String suggestedName = descriptor.getName().asString();
        String name = suggestedName;
        DeclarationDescriptor containing = descriptor;
        while (!nameClashGuard.add(name)) {
            containing = containing.getContainingDeclaration();
            assert containing != null;
            name = suggestedName + '_' + containing.getName().asString();
        }
        return name;
    }

    @Nullable
    public JsNameRef getQualifiedReference(ClassDescriptor descriptor) {
        if (descriptor.getModality() != Modality.FINAL && false) {
            //noinspection ConstantConditions
            return classDescriptorToQualifiedLabel.get(descriptor, null);
        }
        return null;
    }

    @Nullable
    public JsPropertyInitializer translate(@NotNull JetClassOrObject declaration, TranslationContext context) {
        ClassDescriptor descriptor = getClassDescriptor(context().bindingContext(), declaration);
        JsExpression value;
        if (descriptor.getModality() == Modality.FINAL || true) {
            value = new ClassTranslator(declaration, classDescriptorToQualifiedLabel, context).translate();
        }
        else {
            String label = createNameForClass(descriptor);
            JsName name = dummyFunction.getScope().declareName(label);
            JsNameRef qualifiedLabel = openClassDescriptorToQualifiedLabel.get(descriptor);
            if (qualifiedLabel == null) {
                qualifiedLabel = new JsNameRef(name);
                openClassDescriptorToQualifiedLabel.put(descriptor, qualifiedLabel);
            }
            else {
                qualifiedLabel.resolve(name);
            }
            qualifiedLabel.setQualifier(declarationsObjectRef);

            OpenClassInfo item = new OpenClassInfo((JetClass) declaration, descriptor, name.makeRef());

            openList.add(item);
            openClassDescriptorToItem.put(descriptor, item);

            value = qualifiedLabel;

            // not public api classes referenced to internal var _c
            if (!descriptor.getVisibility().isPublicAPI()) {
                return null;
            }
        }

        return new JsPropertyInitializer(context.getNameForDescriptor(descriptor).makeRef(), value);
    }
}

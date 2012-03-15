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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.ClassSortingUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getAllClassesDefinedInNamespace;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Generates a big block where are all the classes(objects representing them) are created.
 */
public final class ClassDeclarationTranslator extends AbstractTranslator {

    @NotNull
    private final List<ClassDescriptor> descriptors;
    @NotNull
    private final Map<JsName, JsName> localToGlobalClassName;
    @NotNull
    private final JsFunction dummyFunction;
    @Nullable
    private JsName declarationsObject = null;
    @Nullable
    private JsStatement declarationsStatement = null;

    public ClassDeclarationTranslator(@NotNull List<ClassDescriptor> descriptors,
                                      @NotNull TranslationContext context) {
        super(context);
        this.descriptors = descriptors;
        this.localToGlobalClassName = new HashMap<JsName, JsName>();
        this.dummyFunction = new JsFunction(context.jsScope());
    }

    public void generateDeclarations() {
        declarationsObject = context().jsScope().declareName(Namer.nameForClassesVariable());
        assert declarationsObject != null;
        declarationsStatement =
                newVar(declarationsObject, generateDummyFunctionInvocation());
    }

    @NotNull
    public JsName getDeclarationsObjectName() {
        assert declarationsObject != null : "Should run generateDeclarations first";
        return declarationsObject;
    }

    @NotNull
    public JsStatement getDeclarationsStatement() {
        assert declarationsStatement != null : "Should run generateDeclarations first";
        return declarationsStatement;
    }

    @NotNull
    private JsInvocation generateDummyFunctionInvocation() {
        List<JsStatement> classDeclarations = generateClassDeclarationStatements();
        classDeclarations.add(new JsReturn(generateReturnedObjectLiteral()));
        dummyFunction.setBody(newBlock(classDeclarations));
        return AstUtil.newInvocation(dummyFunction);
    }

    @NotNull
    private JsObjectLiteral generateReturnedObjectLiteral() {
        JsObjectLiteral returnedValueLiteral = new JsObjectLiteral();
        for (JsName localName : localToGlobalClassName.keySet()) {
            returnedValueLiteral.getPropertyInitializers().add(classEntry(localName));
        }
        return returnedValueLiteral;
    }

    @NotNull
    private JsPropertyInitializer classEntry(@NotNull JsName localName) {
        return new JsPropertyInitializer(localToGlobalClassName.get(localName).makeRef(), localName.makeRef());
    }

    @NotNull
    private List<JsStatement> generateClassDeclarationStatements() {
        List<JsStatement> classDeclarations = new ArrayList<JsStatement>();
        for (JetClass jetClass : getClassDeclarations()) {
            classDeclarations.add(generateDeclaration(jetClass));
        }
        removeAliases();
        return classDeclarations;
    }

    private void removeAliases() {
        for (JetClass jetClass : getClassDeclarations()) {
            ClassDescriptor descriptor = BindingUtils.getClassDescriptor(bindingContext(), jetClass);
            aliaser().removeAliasForDescriptor(descriptor);
        }
    }

    @NotNull
    private List<JetClass> getClassDeclarations() {
        List<JetClass> classes = new ArrayList<JetClass>();
        for (ClassDescriptor classDescriptor : descriptors) {
            classes.add(BindingUtils.getClassForDescriptor(bindingContext(), classDescriptor));
        }
        return ClassSortingUtils.sortUsingInheritanceOrder(classes, bindingContext());
    }

    @NotNull
    private JsStatement generateDeclaration(@NotNull JetClass declaration) {
        JsName localClassName = generateLocalAlias(declaration);
        JsInvocation classDeclarationExpression =
                Translation.translateClassDeclaration(declaration, context());
        return newVar(localClassName, classDeclarationExpression);
    }

    @NotNull
    private JsName generateLocalAlias(@NotNull JetClass declaration) {
        JsName globalClassName = context().getNameForElement(declaration);
        JsName localAlias = dummyFunction.getScope().declareTemporary();
        localToGlobalClassName.put(localAlias, globalClassName);
        ClassDescriptor descriptor = BindingUtils.getClassDescriptor(bindingContext(), declaration);
        aliaser().setAliasForDescriptor(descriptor, localAlias);
        return localAlias;
    }

    @NotNull
    public JsObjectLiteral classDeclarationsForNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        JsObjectLiteral result = new JsObjectLiteral();
        for (ClassDescriptor classDescriptor : getAllClassesDefinedInNamespace(namespaceDescriptor)) {
            result.getPropertyInitializers().add(getClassNameToClassObject(classDescriptor));
        }
        return result;
    }

    @NotNull
    private JsPropertyInitializer getClassNameToClassObject(@NotNull ClassDescriptor classDescriptor) {
        JsName className = context().getNameForDescriptor(classDescriptor);
        JsNameRef alreadyDefinedClassReference = qualified(className, getDeclarationsObjectName().makeRef());
        return new JsPropertyInitializer(className.makeRef(), alreadyDefinedClassReference);
    }
}

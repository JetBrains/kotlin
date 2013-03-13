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

package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.ast.*;

import java.util.List;

import static org.jetbrains.jet.j2k.Converter.modifiersListToModifiersSet;
import static org.jetbrains.jet.j2k.ConverterUtil.isAnnotatedAsNotNull;

public class ElementVisitor extends JavaElementVisitor implements J2KVisitor {
    
    private final Converter myConverter;
    
    @NotNull
    private Element myResult = Element.EMPTY_ELEMENT;

    public ElementVisitor(@NotNull Converter converter) {
        this.myConverter = converter;
    }

    @Override
    @NotNull
    public Converter getConverter() {
        return myConverter;
    }

    @NotNull
    public Element getResult() {
        return myResult;
    }

    @Override
    public void visitLocalVariable(@NotNull PsiLocalVariable variable) {
        super.visitLocalVariable(variable);

        myResult = new LocalVariable(
                new IdentifierImpl(variable.getName()), // TODO
                modifiersListToModifiersSet(variable.getModifierList()),
                getConverter().typeToType(variable.getType(), isAnnotatedAsNotNull(variable.getModifierList())),
                getConverter().createSureCallOnlyForChain(variable.getInitializer(), variable.getType())
        );
    }

    @Override
    public void visitExpressionList(@NotNull PsiExpressionList list) {
        super.visitExpressionList(list);
        myResult = new ExpressionList(
                getConverter().expressionsToExpressionList(list.getExpressions()),
                getConverter().typesToTypeList(list.getExpressionTypes())
        );
    }

    @Override
    public void visitReferenceElement(@NotNull PsiJavaCodeReferenceElement reference) {
        super.visitReferenceElement(reference);

        List<Type> types = getConverter().typesToTypeList(reference.getTypeParameters());
        if (!reference.isQualified()) {
            myResult = new ReferenceElement(
                    new IdentifierImpl(reference.getReferenceName()),
                    types
            );
        }
        else {
            String result = new IdentifierImpl(reference.getReferenceName()).toKotlin();
            PsiElement qualifier = reference.getQualifier();
            while (qualifier != null) {
                PsiJavaCodeReferenceElement p = (PsiJavaCodeReferenceElement) qualifier;
                result = new IdentifierImpl(p.getReferenceName()).toKotlin() + "." + result; // TODO: maybe need to replace by safe call?
                qualifier = p.getQualifier();
            }
            myResult = new ReferenceElement(
                    new IdentifierImpl(result),
                    types
            );
        }
    }

    @Override
    public void visitTypeElement(@NotNull PsiTypeElement type) {
        super.visitTypeElement(type);
        myResult = new TypeElement(getConverter().typeToType(type.getType()));
    }

    @Override
    public void visitTypeParameter(@NotNull PsiTypeParameter classParameter) {
        super.visitTypeParameter(classParameter);
        myResult = new TypeParameter(
                new IdentifierImpl(classParameter.getName()), // TODO
                getConverter().typesToTypeList(classParameter.getExtendsListTypes())
        );
    }

    @Override
    public void visitParameterList(@NotNull PsiParameterList list) {
        super.visitParameterList(list);
        myResult = new ParameterList(
                getConverter().parametersToParameterList(list.getParameters())
        );
    }
}

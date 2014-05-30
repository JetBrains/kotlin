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

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import org.jetbrains.jet.j2k.*
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.ast.types.Type

class ElementVisitor(public val converter: Converter) : JavaElementVisitor() {
    public var result: Element = Element.Empty
        protected set

    override fun visitLocalVariable(variable: PsiLocalVariable) {
        var kType = converter.convertType(variable.getType(), variable.isAnnotatedAsNotNull())
        if (variable.hasModifierProperty(PsiModifier.FINAL) && variable.getInitializer().isDefinitelyNotNull()) {
            kType = kType.convertedToNotNull()
        }
        result = LocalVariable(Identifier(variable.getName()!!),
                                 converter.convertModifierList(variable.getModifierList()),
                                 kType,
                                 converter.convertExpression(variable.getInitializer(), variable.getType()),
                                 converter)
    }

    override fun visitExpressionList(list: PsiExpressionList) {
        result = ExpressionList(converter.convertExpressions(list.getExpressions()))
    }

    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        val types = converter.convertTypes(reference.getTypeParameters())
        if (!reference.isQualified()) {
            result = ReferenceElement(Identifier(reference.getReferenceName()!!), types)
        }
        else {
            var code = Identifier(reference.getReferenceName()!!).toKotlin()
            var qualifier = reference.getQualifier()
            while (qualifier != null) {
                val p = qualifier as PsiJavaCodeReferenceElement
                code = Identifier(p.getReferenceName()!!).toKotlin() + "." + code
                qualifier = p.getQualifier()
            }
            result = ReferenceElement(Identifier(code), types)
        }
    }

    override fun visitTypeElement(`type`: PsiTypeElement) {
        result = TypeElement(converter.convertType(`type`.getType()))
    }

    override fun visitTypeParameter(classParameter: PsiTypeParameter) {
        result = TypeParameter(Identifier(classParameter.getName()!!),
                                 classParameter.getExtendsListTypes().map { converter.convertType(it) })
    }

    override fun visitParameterList(list: PsiParameterList) {
        result = ParameterList(converter.convertParameterList(list.getParameters()))
    }

    override fun visitComment(comment: PsiComment) {
        result = Comment(comment.getText()!!)
    }

    override fun visitWhiteSpace(space: PsiWhiteSpace) {
        result = WhiteSpace(space.getText()!!)
    }
}

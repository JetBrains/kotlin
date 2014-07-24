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

class ElementVisitor(private val converter: Converter) : JavaElementVisitor() {
    private val typeConverter = converter.typeConverter

    public var result: Element = Element.Empty
        protected set

    override fun visitLocalVariable(variable: PsiLocalVariable) {
        val isVal = variable.hasModifierProperty(PsiModifier.FINAL) ||
                variable.getInitializer() == null/* we do not know actually and prefer val until we have better analysis*/ ||
                !variable.hasWriteAccesses(variable.getContainingMethod())
        result = LocalVariable(variable.declarationIdentifier(),
                               converter.convertAnnotations(variable),
                               converter.convertModifiers(variable),
                               converter.variableTypeToDeclare(variable, converter.settings.specifyLocalVariableTypeByDefault, isVal),
                               converter.convertExpression(variable.getInitializer(), variable.getType()),
                               isVal)
    }

    override fun visitExpressionList(list: PsiExpressionList) {
        result = ExpressionList(converter.convertExpressions(list.getExpressions()))
    }

    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        val types = typeConverter.convertTypes(reference.getTypeParameters())
        if (!reference.isQualified()) {
            result = ReferenceElement(Identifier(reference.getReferenceName()!!).assignNoPrototype(), types)
        }
        else {
            var code = Identifier.toKotlin(reference.getReferenceName()!!)
            var qualifier = reference.getQualifier()
            while (qualifier != null) {
                val p = qualifier as PsiJavaCodeReferenceElement
                code = Identifier.toKotlin(p.getReferenceName()!!) + "." + code
                qualifier = p.getQualifier()
            }
            result = ReferenceElement(Identifier(code).assignNoPrototype(), types)
        }
    }

    override fun visitTypeElement(`type`: PsiTypeElement) {
        result = TypeElement(typeConverter.convertType(`type`.getType()))
    }

    override fun visitTypeParameter(classParameter: PsiTypeParameter) {
        result = TypeParameter(classParameter.declarationIdentifier(),
                               classParameter.getExtendsListTypes().map { typeConverter.convertType(it) })
    }

    override fun visitParameterList(list: PsiParameterList) {
        result = converter.convertParameterList(list)
    }
}

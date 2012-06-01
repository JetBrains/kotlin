package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.ast.types.Type
import java.util.List
import org.jetbrains.jet.j2k.isAnnotatedAsNotNull
import org.jetbrains.jet.j2k.isDefinitelyNotNull

public open class ElementVisitor(val myConverter : Converter) : JavaElementVisitor() {
    protected var myResult : Element = Element.EMPTY_ELEMENT

    public fun getConverter() : Converter {
        return myConverter
    }

    public open fun getResult() : Element {
        return myResult
    }

    public override fun visitLocalVariable(variable : PsiLocalVariable?) : Unit {
        val theVariable = variable!!
        var kType = myConverter.typeToType(theVariable.getType(), isAnnotatedAsNotNull(theVariable.getModifierList()))
        if (theVariable.hasModifierProperty(PsiModifier.FINAL) && isDefinitelyNotNull(theVariable.getInitializer())) {
            kType = kType.convertedToNotNull();
        }
        myResult = LocalVariable(Identifier(theVariable.getName()!!),
                Converter.modifiersListToModifiersSet(theVariable.getModifierList()),
                kType,
                myConverter.expressionToExpression(theVariable.getInitializer(), theVariable.getType()))
    }

    public override fun visitExpressionList(list : PsiExpressionList?) : Unit {
        myResult = ExpressionList(myConverter.expressionsToExpressionList(list!!.getExpressions()))
    }

    public override fun visitReferenceElement(reference : PsiJavaCodeReferenceElement?) : Unit {
        val theReference = reference!!
        val types : List<Type> = myConverter.typesToTypeList(theReference.getTypeParameters())
        if (!theReference.isQualified()) {
            myResult = ReferenceElement(Identifier(theReference.getReferenceName()!!), types)
        }
        else {
            var result : String = Identifier(reference?.getReferenceName()!!).toKotlin()
            var qualifier : PsiElement? = theReference.getQualifier()
            while (qualifier != null)
            {
                val p : PsiJavaCodeReferenceElement = (qualifier as PsiJavaCodeReferenceElement)
                result = Identifier(p.getReferenceName()!!).toKotlin() + "." + result
                qualifier = p.getQualifier()
            }
            myResult = ReferenceElement(Identifier(result), types)
        }
    }

    public override fun visitTypeElement(`type` : PsiTypeElement?) : Unit {
        myResult = TypeElement(myConverter.typeToType(`type`!!.getType()))
    }

    public override fun visitTypeParameter(classParameter : PsiTypeParameter?) : Unit {
        myResult = TypeParameter(Identifier(classParameter!!.getName()!!),
                                 classParameter!!.getExtendsListTypes().map { myConverter.typeToType(it) } )
    }

    public override fun visitParameterList(list : PsiParameterList?) : Unit {
        myResult = ParameterList(myConverter.parametersToParameterList(list!!.getParameters()).requireNoNulls())
    }

    public override fun visitComment(comment: PsiComment?) {
        myResult = Comment(comment?.getText()!!)
    }
}

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.ast.types.Type
import java.util.List
import org.jetbrains.jet.j2k.Converter.modifiersListToModifiersSet
import org.jetbrains.jet.j2k.ConverterUtil.isAnnotatedAsNotNull

public open class ElementVisitor(val myConverter : Converter) : JavaElementVisitor(), J2KVisitor {
    private var myResult : Element = Element.EMPTY_ELEMENT

    public override fun getConverter() : Converter {
        return myConverter
    }

    public open fun getResult() : Element {
        return myResult
    }

    public override fun visitLocalVariable(variable : PsiLocalVariable?) : Unit {
        val theVariable = variable!!
        myResult = LocalVariable(IdentifierImpl(theVariable.getName()),
                modifiersListToModifiersSet(theVariable.getModifierList()),
                myConverter.typeToType(theVariable.getType(), isAnnotatedAsNotNull(theVariable.getModifierList())),
                myConverter.createSureCallOnlyForChain(theVariable.getInitializer(), theVariable.getType()))
    }

    public override fun visitExpressionList(list : PsiExpressionList?) : Unit {
        myResult = ExpressionList(myConverter.expressionsToExpressionList(list!!.getExpressions()),
                myConverter.typesToTypeList(list!!.getExpressionTypes()))
    }

    public override fun visitReferenceElement(reference : PsiJavaCodeReferenceElement?) : Unit {
        val theReference = reference!!
        val types : List<Type> = myConverter.typesToTypeList(theReference.getTypeParameters()).requireNoNulls()
        if (!theReference.isQualified()) {
            myResult = ReferenceElement(IdentifierImpl(theReference.getReferenceName()), types)
        }
        else {
            var result : String = IdentifierImpl(reference?.getReferenceName()).toKotlin()
            var qualifier : PsiElement? = theReference.getQualifier()
            while (qualifier != null)
            {
                val p : PsiJavaCodeReferenceElement = (qualifier as PsiJavaCodeReferenceElement)
                result = IdentifierImpl(p.getReferenceName()).toKotlin() + "." + result
                qualifier = p.getQualifier()
            }
            myResult = ReferenceElement(IdentifierImpl(result), types)
        }
    }

    public override fun visitTypeElement(`type` : PsiTypeElement?) : Unit {
        myResult = TypeElement(myConverter.typeToType(`type`!!.getType()))
    }

    public override fun visitTypeParameter(classParameter : PsiTypeParameter?) : Unit {
        myResult = TypeParameter(IdentifierImpl(classParameter!!.getName()),
                                 classParameter!!.getExtendsListTypes().map { myConverter.typeToType(it) } )
    }

    public override fun visitParameterList(list : PsiParameterList?) : Unit {
        myResult = ParameterList(myConverter.parametersToParameterList(list!!.getParameters()))
    }
}

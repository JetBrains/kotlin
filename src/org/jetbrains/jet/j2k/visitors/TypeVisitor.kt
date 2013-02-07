package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.J2KConverterFlags
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.ast.types.*
import java.util.LinkedList
import com.intellij.openapi.util.text.StringUtil
import java.util.ArrayList

public open class TypeVisitor(private val myConverter : Converter) : PsiTypeVisitor<Type>() {
    private var myResult : Type = EmptyType()
    public open fun getResult() : Type {
        return myResult
    }

    public override fun visitPrimitiveType(primitiveType: PsiPrimitiveType?) : Type {
        val name : String = primitiveType?.getCanonicalText()!!
        if (name == "void") {
            myResult = PrimitiveType(Identifier("Unit"))
        }
        else if (Node.PRIMITIVE_TYPES.contains(name)) {
            myResult = PrimitiveType(Identifier(StringUtil.capitalize(name)))
        }
        else {
            myResult = PrimitiveType(Identifier(name))
        }
        return myResult
    }

    public override fun visitArrayType(arrayType: PsiArrayType?) : Type {
        if (myResult is EmptyType) {
            myResult = ArrayType(myConverter.typeToType(arrayType?.getComponentType()), true)
        }

        return myResult
    }

    public override fun visitClassType(classType : PsiClassType?) : Type {
        if (classType == null) return myResult
        val identifier : Identifier = constructClassTypeIdentifier(classType)
        val resolvedClassTypeParams : List<Type> = createRawTypesForResolvedReference(classType)
        if (classType.getParameterCount() == 0 && resolvedClassTypeParams.size() > 0) {
            val myParamList : ArrayList<Type> = ArrayList<Type>()
            if (resolvedClassTypeParams.size() == 1) {
                if ((resolvedClassTypeParams.get(0) as ClassType).`type`.name == "Any") {
                    myParamList.add(StarProjectionType())
                    myResult = ClassType(identifier, myParamList, true)
                }
                else {
                    myResult = ClassType(identifier, resolvedClassTypeParams, true)
                }
            }
            else {
                myResult = ClassType(identifier, resolvedClassTypeParams, true)
            }
        }
        else {
            myResult = ClassType(identifier, myConverter.typesToTypeList(classType.getParameters()), true)
        }
        return myResult
    }

    private fun constructClassTypeIdentifier(classType : PsiClassType) : Identifier {
        val psiClass : PsiClass? = classType.resolve()
        if (psiClass != null) {
            val qualifiedName: String? = psiClass.getQualifiedName()
            if (qualifiedName != null) {
                if (!qualifiedName.equals("java.lang.Object") && myConverter.hasFlag(J2KConverterFlags.FULLY_QUALIFIED_TYPE_NAMES)) {
                    return Identifier(qualifiedName)
                }

                if (qualifiedName.equals(CommonClassNames.JAVA_LANG_ITERABLE)) {
                    return Identifier(CommonClassNames.JAVA_LANG_ITERABLE)
                }

                if (qualifiedName.equals(CommonClassNames.JAVA_UTIL_ITERATOR)) {
                    return Identifier(CommonClassNames.JAVA_UTIL_ITERATOR)
                }
            }
        }

        val classTypeName = createQualifiedName(classType)
        if (classTypeName.isEmpty()) {
            return Identifier(getClassTypeName(classType))
        }

        return Identifier(classTypeName)
    }

    private fun createRawTypesForResolvedReference(classType : PsiClassType) : List<Type> {
        val typeParams = LinkedList<Type>()
        if (classType is PsiClassReferenceType) {
            val reference : PsiJavaCodeReferenceElement? = (classType as PsiClassReferenceType).getReference()
            val resolve : PsiElement? = reference?.resolve()
            if (resolve is PsiClass) {
                for (p : PsiTypeParameter? in (resolve as PsiClass).getTypeParameters()) {
                    val superTypes = p!!.getSuperTypes()
                    val boundType : Type = (if (superTypes.size > 0)
                        ClassType(Identifier(getClassTypeName(superTypes[0])),
                                  myConverter.typesToTypeList(superTypes[0].getParameters()),
                                  true)
                    else
                        StarProjectionType())
                    typeParams.add(boundType)
                }
            }
        }

        return typeParams
    }

    public override fun visitWildcardType(wildcardType : PsiWildcardType?) : Type {
        if (wildcardType!!.isExtends()) {
            myResult = OutProjectionType(myConverter.typeToType(wildcardType.getExtendsBound()))
        }
        else
            if (wildcardType.isSuper()) {
                myResult = InProjectionType(myConverter.typeToType(wildcardType.getSuperBound()))
            }
            else {
                myResult = StarProjectionType()
            }
        return myResult
    }

    public override fun visitEllipsisType(ellipsisType : PsiEllipsisType?) : Type {
        myResult = VarArg(myConverter.typeToType(ellipsisType?.getComponentType()))
        return myResult
    }

    class object {
        private fun createQualifiedName(classType : PsiClassType) : String {
            if (classType is PsiClassReferenceType)
            {
                val reference : PsiJavaCodeReferenceElement? = (classType as PsiClassReferenceType).getReference()
                if (reference != null && reference.isQualified()) {
                    var result : String = Identifier(reference.getReferenceName()!!).toKotlin()
                    var qualifier : PsiElement? = reference.getQualifier()
                    while (qualifier != null)
                    {
                        val p : PsiJavaCodeReferenceElement = (qualifier as PsiJavaCodeReferenceElement)
                        result = Identifier(p.getReferenceName()!!).toKotlin() + "." + result
                        qualifier = p.getQualifier()
                    }
                    return result
                }
            }

            return ""
        }

        private fun getClassTypeName(classType : PsiClassType) : String {
            var canonicalTypeStr : String? = classType.getCanonicalText()
            return when(canonicalTypeStr) {
                CommonClassNames.JAVA_LANG_OBJECT -> "Any"
                CommonClassNames.JAVA_LANG_BYTE -> "Byte"
                CommonClassNames.JAVA_LANG_CHARACTER -> "Char"
                CommonClassNames.JAVA_LANG_DOUBLE -> "Double"
                CommonClassNames.JAVA_LANG_FLOAT -> "Float"
                CommonClassNames.JAVA_LANG_INTEGER -> "Int"
                CommonClassNames.JAVA_LANG_LONG -> "Long"
                CommonClassNames.JAVA_LANG_SHORT -> "Short"
                CommonClassNames.JAVA_LANG_BOOLEAN -> "Boolean"

                else -> (if (classType.getClassName() != null)
                            classType.getClassName()!!
                         else
                            classType.getCanonicalText())!!
            }
        }
    }
}

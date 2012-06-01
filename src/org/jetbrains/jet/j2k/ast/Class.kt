package org.jetbrains.jet.j2k.ast

import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.J2KConverterFlags
import org.jetbrains.jet.j2k.ast.types.ClassType
import org.jetbrains.jet.j2k.ast.types.Type
import java.util.HashSet
import java.util.LinkedList
import java.util.List
import java.util.Set
import java.util.ArrayList

public open class Class(converter : Converter,
                        val name : Identifier,
                        val docComments: List<Node>,
                        modifiers : Set<Modifier>,
                        val typeParameters : List<Element>,
                        val extendsTypes : List<Type>,
                        val baseClassParams : List<Expression>,
                        val implementsTypes : List<Type>,
                        members : List<Node>) : Member(modifiers) {
    val members = getMembers(members, converter)

    open val TYPE: String
        get() = "class"

    private fun getPrimaryConstructor() : Constructor? {
        return members.find { it is Constructor && it.isPrimary } as Constructor?
    }

    open fun primaryConstructorSignatureToKotlin() : String {
        val maybeConstructor : Constructor? = getPrimaryConstructor()
        return if (maybeConstructor != null) maybeConstructor.primarySignatureToKotlin() else "()"
    }

    open fun primaryConstructorBodyToKotlin() : String? {
        val maybeConstructor : Constructor? = getPrimaryConstructor()
        if (maybeConstructor != null && !(maybeConstructor.block?.isEmpty() ?: true)) {
            return maybeConstructor.primaryBodyToKotlin()
        }

        return ""
    }

    private fun hasWhere() : Boolean = typeParameters.any { it is TypeParameter && it.hasWhere() }

    open fun typeParameterWhereToKotlin() : String {
        if (hasWhere()) {
            val wheres = typeParameters.filter { it is TypeParameter }.map { (it as TypeParameter).getWhereToKotlin() }
            return " where " + wheres.makeString(", ") + " "
        }
        return ""
    }

    open fun membersExceptConstructors() : List<Node> = members.filterNot { it is Constructor }

    open fun secondaryConstructorsAsStaticInitFunction() : List<Function> {
        return members.filter { it is Constructor && !it.isPrimary }.map { constructorToInit(it as Function) }
    }

    private fun constructorToInit(f: Function): Function {
        val modifiers : Set<Modifier> = HashSet<Modifier>(f.modifiers)
        modifiers.add(Modifier.STATIC)
        val statements : List<Element> = f.block?.statements ?: arrayList()
        statements.add(ReturnStatement(Identifier("__")))
        val block = Block(statements)
        val constructorTypeParameters : List<Element> = arrayList()
        constructorTypeParameters.addAll(typeParameters)
        constructorTypeParameters.addAll(f.typeParameters)
        return Function(Identifier("init"), arrayList(), modifiers, ClassType(name, constructorTypeParameters, false),
                constructorTypeParameters, f.params, block)
    }

    open fun typeParametersToKotlin() : String = typeParameters.toKotlin(", ", "<", ">")

    open fun baseClassSignatureWithParams() : List<String> {
        if (TYPE.equals("class") && extendsTypes.size() == 1) {
            val baseParams = baseClassParams.toKotlin(", ")
            return arrayList(extendsTypes[0].toKotlin() + "(" + baseParams + ")")
        }
        return extendsTypes.map { it.toKotlin() }
    }

    open fun implementTypesToKotlin() : String {
        val allTypes : List<String> = arrayList()
        allTypes.addAll(baseClassSignatureWithParams())
        allTypes.addAll(implementsTypes.map { it.toKotlin() })
        return if (allTypes.size() == 0)
            ""
        else
            " : " + allTypes.makeString(", ")
    }

    open fun modifiersToKotlin() : String {
        val modifierList : List<Modifier> = arrayList()
        val modifier = accessModifier()
        if (modifier != null) {
            modifierList.add(modifier)
        }
        if (needAbstractModifier()) {
            modifierList.add(Modifier.ABSTRACT)
        }

        if (needOpenModifier()) {
            modifierList.add(Modifier.OPEN)
        }
        return modifierList.toKotlin()
    }

    open fun needOpenModifier() = !modifiers.contains(Modifier.FINAL) && !modifiers.contains(Modifier.ABSTRACT)

    open fun needAbstractModifier() = isAbstract()

    open fun bodyToKotlin() : String {
        return " {\n" + getNonStatic(membersExceptConstructors()).toKotlin("\n") + "\n" + primaryConstructorBodyToKotlin() + "\n" + classObjectToKotlin() + "\n}"
    }

    private fun classObjectToKotlin() : String {
        val staticMembers : List<Node> = arrayList()
        staticMembers.addAll(secondaryConstructorsAsStaticInitFunction())
        staticMembers.addAll(getStatic(membersExceptConstructors()))
        return staticMembers.toKotlin("\n", "class object {\n", "\n}")
    }

    public override fun toKotlin() : String =
        docComments.toKotlin("\n", "", "\n") +
        modifiersToKotlin() +
        TYPE + " " + name.toKotlin() +
        typeParametersToKotlin() +
        primaryConstructorSignatureToKotlin() +
        implementTypesToKotlin() +
        typeParameterWhereToKotlin() +
        bodyToKotlin()

    class object {
        open fun getMembers(members : List<Node>, converter : Converter) : List<Node> {
            if (converter.hasFlag(J2KConverterFlags.SKIP_NON_PUBLIC_MEMBERS)) {
                return members.filter { it is Comment ||
                                        (it as Member).accessModifier() == Modifier.PUBLIC ||
                                        (it as Member).accessModifier() == Modifier.PROTECTED }
            }
            return members
        }

        private fun getStatic(members : List<Node>) : List<Node> {
            return members.filter { it is Member && it.isStatic() }
        }

        private fun getNonStatic(members : List<Node>) : List<Node> {
            return members.filterNot { it is Member && it.isStatic() }
        }
    }
}

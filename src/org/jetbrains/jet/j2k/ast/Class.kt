package org.jetbrains.jet.j2k.ast

import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.J2KConverterFlags
import org.jetbrains.jet.j2k.ast.types.ClassType
import org.jetbrains.jet.j2k.ast.types.Type
import org.jetbrains.jet.j2k.util.AstUtil
import java.util.HashSet
import java.util.LinkedList
import java.util.List
import java.util.Set
import org.jetbrains.jet.j2k.util.AstUtil.*
import java.util.ArrayList

public open class Class(converter : Converter,
                        val name : Identifier,
                        modifiers : Set<String>,
                        val typeParameters : List<Element>,
                        val extendsTypes : List<Type>,
                        val baseClassParams : List<Expression>,
                        val implementsTypes : List<Type>,
                        members : List<Member>) : Member(modifiers) {
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
        var maybeConstructor : Constructor? = getPrimaryConstructor()
        if (maybeConstructor != null && !(maybeConstructor?.block?.isEmpty() ?: true))
        {
            return maybeConstructor?.primaryBodyToKotlin()
        }

        return ""
    }

    private fun hasWhere() : Boolean = typeParameters.any { it is TypeParameter && it.hasWhere() }

    open fun typeParameterWhereToKotlin() : String {
        if (hasWhere())
        {
            val wheres = typeParameters.filter { it is TypeParameter }.map { (it as TypeParameter).getWhereToKotlin() }
            return " where " + wheres.makeString(", ") + " "
        }
        return ""
    }

    open fun membersExceptConstructors() : List<Member> = members.filterNot { it is Constructor }

    open fun secondaryConstructorsAsStaticInitFunction() : List<Function> {
        return members.filter { it is Constructor && !it.isPrimary }.map { constructorToInit(it as Function) }
    }

    private fun constructorToInit(f: Function): Function {
        val modifiers : Set<String> = HashSet<String>(f.getModifiers())
        modifiers.add(Modifier.STATIC)
        val statements : List<Statement> = f.block?.statements ?: arrayList()
        statements.add(ReturnStatement(Identifier("__")))
        val block : Block = Block(statements)
        val constructorTypeParameters : List<Element> = arrayList()
        constructorTypeParameters.addAll(typeParameters)
        constructorTypeParameters.addAll(f.typeParameters)
        return Function(Identifier("init"), modifiers, ClassType(name, constructorTypeParameters, false),
                constructorTypeParameters, f.params, block)
    }

    open fun typeParametersToKotlin() : String {
        return (if (typeParameters.size() > 0)
            "<" + typeParameters.map { it.toKotlin() }.makeString(", ") + ">"
        else
            "")
    }

    open fun baseClassSignatureWithParams() : List<String?> {
        if (TYPE.equals("class") && extendsTypes.size() == 1)
        {
            val baseParams = baseClassParams.map { it.toKotlin() }.makeString(", ")
            return arrayList(extendsTypes[0].toKotlin() + "(" + baseParams + ")")
        }
        return nodesToKotlin(extendsTypes)
    }

    open fun implementTypesToKotlin() : String {
        val allTypes : List<String?> = arrayList()
        allTypes.addAll(baseClassSignatureWithParams())
        allTypes.addAll(nodesToKotlin(implementsTypes))
        return if (allTypes.size() == 0)
            ""
        else
            " : " + allTypes.makeString(", ")
    }

    open fun modifiersToKotlin() : String {
        val modifierList : List<String> = arrayList()
        modifierList.add(accessModifier())
        if (needAbstractModifier())
        {
            modifierList.add(Modifier.ABSTRACT)
        }

        if (needOpenModifier())
        {
            modifierList.add(Modifier.OPEN)
        }

        if (modifierList.size() > 0)
        {
            return modifierList.makeString(" ") + " "
        }

        return ""
    }

    open fun needOpenModifier() = !myModifiers.contains(Modifier.FINAL) && !myModifiers.contains(Modifier.ABSTRACT)

    open fun needAbstractModifier() = isAbstract()

    open fun bodyToKotlin() : String {
        return " {\n" + AstUtil.joinNodes(getNonStatic(membersExceptConstructors()), "\n") + "\n" + primaryConstructorBodyToKotlin() + "\n" + classObjectToKotlin() + "\n}"
    }

    private fun classObjectToKotlin() : String {
        val staticMembers : List<Member> = arrayList()
        staticMembers.addAll(secondaryConstructorsAsStaticInitFunction())
        staticMembers.addAll(getStatic(membersExceptConstructors()))
        if (staticMembers.size() > 0)
        {
            return "class object {\n" + AstUtil.joinNodes(staticMembers, "\n") + "\n}"
        }

        return ""
    }
    public override fun toKotlin() : String =
        modifiersToKotlin() +
        TYPE + " " + name.toKotlin() +
        typeParametersToKotlin() +
        primaryConstructorSignatureToKotlin() +
        implementTypesToKotlin() +
        typeParameterWhereToKotlin() +
        bodyToKotlin()

    class object {
        open fun getMembers(members : List<Member>, converter : Converter) : List<Member> {
            if (converter.hasFlag(J2KConverterFlags.SKIP_NON_PUBLIC_MEMBERS).sure())
            {
                val withoutPrivate : List<Member> = arrayList()
                for (m : Member in members)
                {
                    if (m.accessModifier().equals("public") || m.accessModifier().equals("protected"))
                    {
                        withoutPrivate.add(m)
                    }

                }
                return withoutPrivate
            }
            return members
        }

        private fun getStatic(members : List<out Member>) : List<Member> {
            val result : List<Member> = arrayList()
            for (m in members)
                if (m.isStatic())
                    result.add(m)

            return result
        }

        private fun getNonStatic(members : List<out Member>) : List<Member> {
            val result : List<Member> = arrayList()
            for (m in members)
                if (!m.isStatic())
                {
                    result.add(m)
                }

            return result
        }
    }
}

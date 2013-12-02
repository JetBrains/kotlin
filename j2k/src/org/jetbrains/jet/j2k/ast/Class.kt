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

package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.types.ClassType
import org.jetbrains.jet.j2k.ast.types.Type
import java.util.HashSet
import java.util.ArrayList

public open class Class(val converter: Converter,
                        val name: Identifier,
                        docComment: Comment?,
                        modifiers: Set<Modifier>,
                        val typeParameterList: TypeParameterList,
                        val extendsTypes: List<Type>,
                        val baseClassParams: List<Expression>,
                        val implementsTypes: List<Type>,
                        val members: List<Node>) : Member(docComment, modifiers) {
    open val TYPE: String
        get() = "class"

    private fun getPrimaryConstructor(): Constructor? {
        return members.find { it is Constructor && it.isPrimary } as Constructor?
    }

    open fun primaryConstructorSignatureToKotlin(): String {
        val maybeConstructor: Constructor? = getPrimaryConstructor()
        return if (maybeConstructor != null) maybeConstructor.primarySignatureToKotlin() else "()"
    }

    fun primaryConstructorBodyToKotlin(): String? {
        val maybeConstructor = getPrimaryConstructor()
        if (maybeConstructor != null && !(maybeConstructor.block?.isEmpty() ?: true)) {
            return maybeConstructor.primaryBodyToKotlin() + "\n"
        }

        return ""
    }

    fun membersExceptConstructors(): List<Node> = members.filterNot { it is Constructor }

    fun secondaryConstructorsAsStaticInitFunction(): List<Function> {
        return members.filter { it is Constructor && !it.isPrimary }.map { constructorToInit(it as Function) }
    }

    private fun constructorToInit(f: Function): Function {
        val modifiers = HashSet<Modifier>(f.modifiers)
        modifiers.add(Modifier.STATIC)
        val statements = ArrayList(f.block?.statements ?: ArrayList())
        statements.add(ReturnStatement(Identifier("__")))
        val block = Block(statements)
        val constructorTypeParameters = ArrayList<TypeParameter>()
        constructorTypeParameters.addAll(typeParameterList.parameters)
        constructorTypeParameters.addAll(f.typeParameterList.parameters)
        return Function(converter, Identifier("init"), null, modifiers,
                        ClassType(name, constructorTypeParameters, false, converter),
                        TypeParameterList(constructorTypeParameters), f.params, block)
    }

    fun baseClassSignatureWithParams(): List<String> {
        if (TYPE.equals("class") && extendsTypes.size() == 1) {
            val baseParams = baseClassParams.toKotlin(", ")
            return arrayListOf(extendsTypes[0].toKotlin() + "(" + baseParams + ")")
        }
        return extendsTypes.map { it.toKotlin() }
    }

    fun implementTypesToKotlin(): String {
        val allTypes = ArrayList<String>()
        allTypes.addAll(baseClassSignatureWithParams())
        allTypes.addAll(implementsTypes.map { it.toKotlin() })
        return if (allTypes.size() == 0)
            ""
        else
            " : " + allTypes.makeString(", ")
    }

    fun modifiersToKotlin(): String {
        val modifierList = ArrayList<Modifier>()
        val modifier = accessModifier()
        if (modifier != null) {
            modifierList.add(modifier)
        }
        if (isAbstract()) {
            modifierList.add(Modifier.ABSTRACT)
        }
        else if (needsOpenModifier()) {
            modifierList.add(Modifier.OPEN)
        }
        return modifierList.toKotlin()
    }

    open fun isDefinitelyFinal() = modifiers.contains(Modifier.FINAL)

    open fun needsOpenModifier() = !isDefinitelyFinal() && converter.settings.openByDefault

    fun bodyToKotlin(): String {
        return " {\n" + getNonStatic(membersExceptConstructors()).toKotlin("\n", "", "\n") + primaryConstructorBodyToKotlin() + classObjectToKotlin() + "}"
    }

    fun classObjectToKotlin(): String {
        val staticMembers = ArrayList<Node>()
        staticMembers.addAll(secondaryConstructorsAsStaticInitFunction())
        staticMembers.addAll(getStatic(membersExceptConstructors()))
        return staticMembers.toKotlin("\n", "class object {\n", "\n}")
    }

    override fun toKotlin(): String =
            docCommentToKotlin() +
            modifiersToKotlin() +
            TYPE + " " + name.toKotlin() +
            typeParameterList.toKotlin() +
            primaryConstructorSignatureToKotlin() +
            implementTypesToKotlin() +
            typeParameterList.whereToKotlin().withPrefix(" ") +
            bodyToKotlin()


    private fun getStatic(members: List<Node>): List<Node> {
        return members.filter { it is Member && it.isStatic() }
    }

    private fun getNonStatic(members: List<Node>): List<Node> {
        return members.filterNot { it is Member && it.isStatic() }
    }
}

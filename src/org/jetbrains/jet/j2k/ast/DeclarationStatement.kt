package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.util.AstUtil
import java.util.LinkedList
import java.util.List

public open class DeclarationStatement(val elements: List<Element>): Statement() {
    public override fun toKotlin(): String {
        return elements.filter { it is LocalVariable }.map { convertDeclaration(it as LocalVariable) }.makeString("\n")
    }

    private fun convertDeclaration(v: LocalVariable): String {
        val varKeyword: String? = (if (v.hasModifier(Modifier.FINAL))
            "val"
        else
            "var")
        return varKeyword + " " + v.toKotlin()
    }
}

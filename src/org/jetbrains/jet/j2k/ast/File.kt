package org.jetbrains.jet.j2k.ast

import java.util.List

public open class File(val packageName: String,
                       val imports: List<Import>,
                       val body: List<Node>,
                       val mainFunction: String): Node() {

    public override fun toKotlin(): String {
        val common: String = imports.toKotlin("\n") + "\n\n" + body.toKotlin("\n") + "\n" + mainFunction
        if (packageName.isEmpty()) {
            return common
        }

        return "package " + packageName + "\n" + common
    }
}

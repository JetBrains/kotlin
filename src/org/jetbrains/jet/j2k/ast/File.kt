package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.util.AstUtil
import java.util.List

public open class File(val packageName: String,
                       val imports: List<Import>,
                       val classes: List<Class>,
                       val mainFunction: String): Node() {

    public override fun toKotlin(): String {
        val common: String = AstUtil.joinNodes(imports, "\n") + "\n\n" + AstUtil.joinNodes(classes, "\n") + "\n" + mainFunction
        if (packageName.isEmpty()) {
            return common
        }

        return "package " + packageName + "\n" + common
    }
}

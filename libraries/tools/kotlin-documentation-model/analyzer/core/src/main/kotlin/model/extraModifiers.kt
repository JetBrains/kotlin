package org.jetbrains.dokka.model

sealed class ExtraModifiers(val name: String) {

    sealed class KotlinOnlyModifiers(name: String) : ExtraModifiers(name) {
        object Inline : KotlinOnlyModifiers("inline")
        object Value : KotlinOnlyModifiers("value")
        object Infix : KotlinOnlyModifiers("infix")
        object External : KotlinOnlyModifiers("external")
        object Suspend : KotlinOnlyModifiers("suspend")
        object Reified : KotlinOnlyModifiers("reified")
        object CrossInline : KotlinOnlyModifiers("crossinline")
        object NoInline : KotlinOnlyModifiers("noinline")
        object Override : KotlinOnlyModifiers("override")
        object Data : KotlinOnlyModifiers("data")
        object Const : KotlinOnlyModifiers("const")
        object Inner : KotlinOnlyModifiers("inner")
        object LateInit : KotlinOnlyModifiers("lateinit")
        object Operator : KotlinOnlyModifiers("operator")
        object TailRec : KotlinOnlyModifiers("tailrec")
        object VarArg : KotlinOnlyModifiers("vararg")
        object Fun : KotlinOnlyModifiers("fun")
    }

    sealed class JavaOnlyModifiers(name: String) : ExtraModifiers(name) {
        object Static : JavaOnlyModifiers("static")
        object Native : JavaOnlyModifiers("native")
        object Synchronized : JavaOnlyModifiers("synchronized")
        object StrictFP : JavaOnlyModifiers("strictfp")
        object Transient : JavaOnlyModifiers("transient")
        object Volatile : JavaOnlyModifiers("volatile")
        object Transitive : JavaOnlyModifiers("transitive")
    }

    companion object {
        fun valueOf(str: String) = when (str) {
            "inline" -> KotlinOnlyModifiers.Inline
            "value" -> KotlinOnlyModifiers.Value
            "infix" -> KotlinOnlyModifiers.Infix
            "external" -> KotlinOnlyModifiers.External
            "suspend" -> KotlinOnlyModifiers.Suspend
            "reified" -> KotlinOnlyModifiers.Reified
            "crossinline" -> KotlinOnlyModifiers.CrossInline
            "noinline" -> KotlinOnlyModifiers.NoInline
            "override" -> KotlinOnlyModifiers.Override
            "data" -> KotlinOnlyModifiers.Data
            "const" -> KotlinOnlyModifiers.Const
            "inner" -> KotlinOnlyModifiers.Inner
            "lateinit" -> KotlinOnlyModifiers.LateInit
            "operator" -> KotlinOnlyModifiers.Operator
            "tailrec" -> KotlinOnlyModifiers.TailRec
            "vararg" -> KotlinOnlyModifiers.VarArg
            "static" -> JavaOnlyModifiers.Static
            "native" -> JavaOnlyModifiers.Native
            "synchronized" -> JavaOnlyModifiers.Synchronized
            "strictfp" -> JavaOnlyModifiers.StrictFP
            "transient" -> JavaOnlyModifiers.Transient
            "volatile" -> JavaOnlyModifiers.Volatile
            "transitive" -> JavaOnlyModifiers.Transitive
            "fun" -> KotlinOnlyModifiers.Fun
            else -> throw IllegalArgumentException("There is no Extra Modifier for given name $str")
        }
    }
}
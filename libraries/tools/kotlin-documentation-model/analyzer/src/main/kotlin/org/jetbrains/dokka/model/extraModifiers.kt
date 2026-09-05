/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

public sealed class ExtraModifiers(public val name: String) {

    public sealed class KotlinOnlyModifiers(name: String) : ExtraModifiers(name) {
        public object Inline : KotlinOnlyModifiers("inline")
        public object Value : KotlinOnlyModifiers("value")
        public object Infix : KotlinOnlyModifiers("infix")
        public object External : KotlinOnlyModifiers("external")
        public object Suspend : KotlinOnlyModifiers("suspend")
        public object Reified : KotlinOnlyModifiers("reified")
        public object CrossInline : KotlinOnlyModifiers("crossinline")
        public object NoInline : KotlinOnlyModifiers("noinline")
        public object Override : KotlinOnlyModifiers("override")
        public object Data : KotlinOnlyModifiers("data")
        public object Const : KotlinOnlyModifiers("const")
        public object Inner : KotlinOnlyModifiers("inner")
        public object LateInit : KotlinOnlyModifiers("lateinit")
        public object Operator : KotlinOnlyModifiers("operator")
        public object TailRec : KotlinOnlyModifiers("tailrec")
        public object VarArg : KotlinOnlyModifiers("vararg")
        public object Fun : KotlinOnlyModifiers("fun")
    }

    public sealed class JavaOnlyModifiers(name: String) : ExtraModifiers(name) {
        public object Static : JavaOnlyModifiers("static")
        public object Native : JavaOnlyModifiers("native")
        public object Synchronized : JavaOnlyModifiers("synchronized")
        public object StrictFP : JavaOnlyModifiers("strictfp")
        public object Transient : JavaOnlyModifiers("transient")
        public object Volatile : JavaOnlyModifiers("volatile")
        public object Transitive : JavaOnlyModifiers("transitive")
    }

    public companion object {
        public fun valueOf(str: String): ExtraModifiers = when (str) {
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

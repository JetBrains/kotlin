package org.jetbrains.jet.j2k.ast

public abstract class Member(val modifiers : Set<Modifier>) : Node() {
    open fun accessModifier() : Modifier? {
        return modifiers.find { m -> m == Modifier.PUBLIC || m == Modifier.PROTECTED || m == Modifier.PRIVATE }
    }

    public open fun isAbstract() : Boolean = modifiers.contains(Modifier.ABSTRACT)
    public open fun isStatic() : Boolean = modifiers.contains(Modifier.STATIC)
}

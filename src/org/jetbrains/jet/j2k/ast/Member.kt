package org.jetbrains.jet.j2k.ast

import java.util.Set

public abstract class Member(val myModifiers : Set<String>) : Node() {
    open fun accessModifier() : String {
        return myModifiers.find { m -> m == Modifier.PUBLIC || m == Modifier.PROTECTED || m == Modifier.PRIVATE } ?: ""
    }

    public open fun isAbstract() : Boolean = myModifiers.contains(Modifier.ABSTRACT)
    public open fun isStatic() : Boolean = myModifiers.contains(Modifier.STATIC)
    public open fun getModifiers() : Set<String> = myModifiers
}

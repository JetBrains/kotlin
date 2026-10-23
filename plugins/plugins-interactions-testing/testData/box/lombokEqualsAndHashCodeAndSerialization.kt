// ENABLE_SERIALIZATION
// ENABLE_LOMBOK
// WITH_STDLIB
// ISSUE: KT-90086

import lombok.EqualsAndHashCode

interface Animal

@EqualsAndHashCode
class Dog(val name: String) : Animal

fun box(): String = if (Dog("Rex") == Dog("Rex")) "OK" else "Fail"

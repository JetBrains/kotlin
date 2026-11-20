// ENABLE_SERIALIZATION
// ENABLE_LOMBOK
// WITH_STDLIB
// ISSUE: KT-90086

import lombok.Data

@Data(staticConstructor = "create")
class Dog(val name: String) {
    // The factory is built from the primary constructor parameter types while the supertypes of this companion
    // object are still being resolved
    companion object
}

fun box(): String = if (Dog.create("Rex").name == "Rex") "OK" else "Fail"

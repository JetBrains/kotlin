// FIR_DUMP

import lombok.Builder

@Builder
class ExplicitBuilderWithTypeParametersLessThanRequired<K>(val p: K) {
    abstract class ExplicitBuilderWithTypeParametersLessThanRequiredBuilder
}

@Builder
class ExplicitBuilderWithTypeParametersMoreThanRequired<K>(val p: K) {
    abstract class ExplicitBuilderWithTypeParametersMoreThanRequiredBuilder<T1, T2>
}

fun main() {
    val obj = ExplicitBuilderWithTypeParametersLessThanRequired.builder<Int>().<!UNRESOLVED_REFERENCE!>build<!>()
    val obj2 = ExplicitBuilderWithTypeParametersMoreThanRequired.builder<String>().<!UNRESOLVED_REFERENCE!>build<!>()
}

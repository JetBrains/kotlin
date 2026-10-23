// FIR_DUMP

import lombok.Builder

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class ExplicitBuilderWithTypeParametersLessThanRequired<K>(val p: K) {
    abstract class ExplicitBuilderWithTypeParametersLessThanRequiredBuilder
}

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class ExplicitBuilderWithTypeParametersMoreThanRequired<K>(val p: K) {
    abstract class ExplicitBuilderWithTypeParametersMoreThanRequiredBuilder<T1, T2>
}

fun main() {
    val obj = ExplicitBuilderWithTypeParametersLessThanRequired.<!UNRESOLVED_REFERENCE!>builder<!><Int>().build()
    val obj2 = ExplicitBuilderWithTypeParametersMoreThanRequired.<!UNRESOLVED_REFERENCE!>builder<!><String>().build()
}

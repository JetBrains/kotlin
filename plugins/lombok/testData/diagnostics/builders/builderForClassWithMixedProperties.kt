// ISSUE: KT-89739

import lombok.Builder;

@Builder
class BuilderExample2(val y: kotlin.String?) {
    var x: Int = 0

    fun test() {
        val ex: BuilderExample2? = BuilderExample2.builder()
            // Does not work because of mixed constructor & regular mutable properties
            .<!UNRESOLVED_REFERENCE!>x<!>(42)
            .y("str")
            .build()
    }
}

// ISSUE: KT-89739

import lombok.Builder;

@Builder
class Mutable {
    var privateSetter: String = ""
        private set

    private var privateProperty: String = ""

    fun result(): String = privateSetter + privateProperty
}

fun box(): String = Mutable.builder().privateSetter("O").privateProperty("K").build().result()

fun interface KotlinFace {
    fun single()
}

fun useSam(kf: KotlinFace) {}

fun callSam() {
    useSam(kf = <caret>KotlinFace {})
}
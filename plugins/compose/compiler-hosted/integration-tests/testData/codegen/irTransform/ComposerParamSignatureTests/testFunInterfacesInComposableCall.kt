fun interface MeasurePolicy {
    fun compute(value: Int): Unit
}

@NonRestartableComposable
@Composable fun Text() {
    Layout { value ->
        println(value)
    }
}

@Composable inline fun Layout(policy: MeasurePolicy) {
    policy.compute(0)
}

fun used(x: Any?) {}

package kt38641

// See https://youtrack.jetbrains.com/issue/KT-38641.
class KT38641 {
    class IntType {
        var description = 42
    }

    class Val {
        val description = "val"
    }

    class Var {
        var description = "var"
    }

    class TwoProperties {
        val description = "description"
        val description_ = "description_"
    }

    abstract class OverrideVal {
        abstract val description: String
    }

    interface OverrideVar {
        var description: String
    }
}

fun getOverrideValDescription(impl: KT38641.OverrideVal) = impl.description

fun getOverrideVarDescription(impl: KT38641.OverrideVar) = impl.description
fun setOverrideVarDescription(impl: KT38641.OverrideVar, newValue: String) {
    impl.description = newValue
}

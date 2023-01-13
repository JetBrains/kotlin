package lib

@JvmInline
value class IC private constructor(val value: String) {
    companion object {
        fun of(value: String) = IC(value)
    }
}

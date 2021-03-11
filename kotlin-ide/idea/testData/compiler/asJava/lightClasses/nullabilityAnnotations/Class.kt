// Class

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class Class {
    fun notNull(a: String): String = ""
    fun nullable(a: String?): String? = ""

    @NotNull fun notNullWithNN(): String = ""
    @Nullable fun notNullWithN(): String = ""

    @Nullable fun nullableWithN(): String? = ""
    @NotNull fun nullableWithNN(): String? = ""

    val nullableVal: String? = { "" }()
    var nullableVar: String? = { "" }()
    val notNullVal: String = { "" }()
    var notNullVar: String = { "" }()

    val notNullValWithGet: String
        @[Nullable] get() = ""

    var notNullVarWithGetSet: String
        @[Nullable] get() = ""
        @[Nullable] set(v) {}

    val nullableValWithGet: String?
        @[NotNull] get() = ""

    var nullableVarWithGetSet: String?
        @[NotNull] get() = ""
        @[NotNull] set(v) {}

    private val privateNN: String = { "" }()
    private val privateN: String? = { "" }()

    lateinit var lateInitVar: String
}
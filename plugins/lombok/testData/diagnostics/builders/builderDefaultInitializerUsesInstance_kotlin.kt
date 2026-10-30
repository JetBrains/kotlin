// ISSUE: KT-90590
// WITH_STDLIB
import lombok.Builder

fun topLevel(): Int = 1

@Builder
class Entity {
    @Builder.Default
    var memberCall: Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>compute()<!>

    @Builder.Default
    var explicitThis: String = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>this.toString()<!>

    @Builder.Default
    var lambda: () -> Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>{ memberCall }<!>

    @Builder.Default
    var list: List<Int> = listOf(1, topLevel(), COMPANION_CONST)

    @Builder.Default
    var anonymous: Any = object {
        override fun toString(): String = this.hashCode().toString()
    }

    fun compute(): Int = 42

    companion object {
        const val COMPANION_CONST = 3
    }
}

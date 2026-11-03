// ISSUE: KT-90590
// WITH_STDLIB
import lombok.Builder

fun topLevel(): Int = 1

open class Base {
    fun baseCall(): Int = 2
}

@Builder
class Entity : Base() {
    @Builder.Default
    var memberCall: Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>compute()<!>

    @Builder.Default
    var explicitThis: String = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>this.toString()<!>

    // A lambda captures the instance when it is created, which `build()` does without one, called in place or not
    @Builder.Default
    var lambda: () -> Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>{ memberCall }<!>

    @Builder.Default
    var lambdaWithExplicitThis: () -> String = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>{ this.toString() }<!>

    @Builder.Default
    var lambdaWithLabeledThis: () -> String = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>{ this@Entity.toString() }<!>

    @Builder.Default
    var nestedLambda: () -> () -> Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>{ { memberCall } }<!>

    @Builder.Default
    var anonymousFunction: () -> Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>fun(): Int = memberCall<!>

    @Builder.Default
    var lambdaWithoutInstance: () -> Int = { topLevel() + COMPANION_CONST }

    @Builder.Default
    var inPlaceLambda: Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>topLevel().let { it + memberCall }<!>

    @Builder.Default
    var inPlaceLambdaWithoutInstance: Int = topLevel().let { it + COMPANION_CONST }

    // Resolves to `T.run` with the implicit `this@Entity` receiver, not to the receiver-less `run`
    @Builder.Default
    var implicitReceiverRun: Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>run { topLevel() }<!>

    @Builder.Default
    var explicitWith: Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>with(this) { topLevel() }<!>

    @Builder.Default
    var also: Any = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>this.also { }<!>

    @Builder.Default
    var superCall: Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>super.hashCode()<!>

    @Builder.Default
    var inheritedCall: Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>baseCall()<!>

    @Builder.Default
    var implicitBoundReference: () -> Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>::compute<!>

    @Builder.Default
    var explicitBoundReference: () -> Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>this::compute<!>

    @Builder.Default
    var unboundReference: (Entity) -> Int = Entity::compute

    @Builder.Default
    var topLevelReference: () -> Int = ::topLevel

    @Builder.Default
    var list: List<Int> = listOf(1, topLevel(), COMPANION_CONST)

    @Builder.Default
    var anonymous: Any = object {
        override fun toString(): String = this.hashCode().toString()
    }

    @Builder.Default
    var anonymousCapturingOuter: Any = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>object<!> {
        override fun toString(): String = memberCall.toString()
    }

    fun compute(): Int = 42

    companion object {
        const val COMPANION_CONST = 3
    }
}

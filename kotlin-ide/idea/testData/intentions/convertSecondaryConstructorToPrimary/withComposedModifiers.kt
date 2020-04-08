annotation class AnnParam

annotation class AnnProperty

abstract class WithComposedModifiers {
    @AnnProperty
    open val x: Array<out String>

    constructor<caret>(@AnnParam vararg x: String) {
        this.x = x
    }
}
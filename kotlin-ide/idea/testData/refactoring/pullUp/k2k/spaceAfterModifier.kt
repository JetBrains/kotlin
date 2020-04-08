open class Upper

open class <caret>Lower : Upper() {
    // INFO: {"checked": "true", "toAbstract": "true"}
    protected val moving: Int = 0
}
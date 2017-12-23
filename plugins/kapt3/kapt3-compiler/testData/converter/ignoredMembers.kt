import kotlinx.kapt.*

class Test {
    @KaptIgnored
    fun ignoredFun() {}

    @KaptIgnored @get:KaptIgnored
    val ignoredProperty: String = ""

    fun nonIgnoredFun() {}

    val nonIgnoredProperty: String = ""
}
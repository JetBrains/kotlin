// "Make OClass.overrideMe open" "true"

expect open class OClass() {
    val overrideMe: String
}

class Another: OClass() {
    override<caret> val overrideMe = ""
}
// "Make OClass.overrideMe open" "true"

actual open class OClass actual constructor() {
    actual val overrideMe: String = ""
}

class Another: OClass() {
    override<caret> val overrideMe = ""
}
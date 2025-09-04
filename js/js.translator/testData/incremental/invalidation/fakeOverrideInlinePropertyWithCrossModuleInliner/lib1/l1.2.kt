abstract class AbstractClassA {
    inline val propertyWithGetter: String
        get() = "1"

    inline var propertyWithSetter: String
        get() = savedStr
        set(str) {
            savedStr = "$str 2"
        }

    var savedStr = "empty"
}

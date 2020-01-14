internal annotation class Anon(
        val stringArray: Array<String>, val intArray: IntArray, // string
        val string: String
)

@Anon(string = "a", stringArray = ["a", "b"], intArray = [1, 2])
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FIELD)
internal annotation class I

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
internal annotation class J

@Target
internal annotation class K

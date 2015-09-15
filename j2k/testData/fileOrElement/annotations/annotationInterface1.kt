annotation internal class Anon(val stringArray: Array<String>, val intArray: IntArray, // string
                               val string: String)

Anon(string = "a", stringArray = arrayOf("a", "b"), intArray = intArrayOf(1, 2))
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FIELD)
annotation internal class I

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation internal class J

@Target
annotation internal class K

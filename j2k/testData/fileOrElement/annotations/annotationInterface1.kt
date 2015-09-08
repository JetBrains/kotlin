annotation class Anon(public val stringArray: Array<String>, public val intArray: IntArray, // string
                      public val string: String)

Anon(string = "a", stringArray = arrayOf("a", "b"), intArray = intArrayOf(1, 2))
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FIELD)
annotation class I

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class J

@Target
annotation class K

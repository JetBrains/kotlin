annotation class Anon(public val stringArray: Array<String>, public val intArray: IntArray, // string
                      public val string: String)

Anon(string = "a", stringArray = arrayOf("a", "b"), intArray = intArrayOf(1, 2))
target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FIELD)
annotation class I

target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class J

target
annotation class K

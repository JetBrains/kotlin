@Target(AnnotationTarget.FIELD)
annotation class FieldAnno

@Target(AnnotationTarget.PROPERTY)
annotation class PropertyAnno

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParameterAnno

annotation class Anno

class Foo(@FieldAnno @PropertyAnno @ParameterAnno @Anno val a: String)

class Bar {
    @FieldAnno @PropertyAnno @Anno
    val a: String = ""
}

class Baz {
    @FieldAnno @Anno
    @JvmField
    val a: String = ""
}
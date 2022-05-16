package cases.marker

@Target(AnnotationTarget.FIELD)
annotation class HiddenField

@Target(AnnotationTarget.PROPERTY)
annotation class HiddenProperty

public class Foo {
    // HiddenField will be on the field
    @HiddenField
    var bar1 = 42

    // HiddenField will be on a synthetic `$annotations()` method
    @HiddenProperty
    var bar2 = 42
}


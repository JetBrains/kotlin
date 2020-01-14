internal annotation class TestAnnotation

class Test(
        @param:TestAnnotation @field:TestAnnotation
        @get:TestAnnotation
        @set:TestAnnotation
        var arg: String?
)

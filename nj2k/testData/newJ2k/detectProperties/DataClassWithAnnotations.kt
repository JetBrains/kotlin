internal annotation class TestAnnotationField
internal annotation class TestAnnotationParam
internal annotation class TestAnnotationGet
internal annotation class TestAnnotationSet
class Test(
        @field:TestAnnotationField @set:TestAnnotationSet
        @get:TestAnnotationGet
        @param:TestAnnotationParam var arg: String
)
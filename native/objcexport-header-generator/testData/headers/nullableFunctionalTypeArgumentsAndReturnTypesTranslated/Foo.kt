interface ParameterType
interface ReturnType

interface Foo {
    val func: ((parameterType: ParameterType) -> ReturnType)?
}
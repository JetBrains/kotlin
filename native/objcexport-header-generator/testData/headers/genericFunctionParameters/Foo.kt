class Foo<ClassParameter> {

    fun List<ClassParameter>.listExtensionWithClassParameter(list: List<ClassParameter>): List<ClassParameter> = list
    fun <MethodParameter> List<MethodParameter>.listExtensionWithMethodParameter(list: List<MethodParameter>): List<MethodParameter> = list

    fun ClassParameter.classParameterExtension(param: ClassParameter): ClassParameter = param
    fun <MethodParameter> MethodParameter.methodParameterExtension(param: MethodParameter): MethodParameter = param
}
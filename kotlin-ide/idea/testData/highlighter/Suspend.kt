// IGNORE_FIR
// EXPECTED_DUPLICATED_HIGHLIGHTING

val <info descr="null" textAttributesKey="KOTLIN_PACKAGE_PROPERTY">fnType</info> : <info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> () -> <info descr="null" textAttributesKey="KOTLIN_OBJECT">Unit</info> = {}

val <info descr="null" textAttributesKey="KOTLIN_PACKAGE_PROPERTY">fnFnType</info>: () -> <info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> () -> <info descr="null" textAttributesKey="KOTLIN_OBJECT">Unit</info> = {  -> {}}

<info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> fun <info descr="null" textAttributesKey="KOTLIN_FUNCTION_DECLARATION">inSuspend</info>(<info descr="null" textAttributesKey="KOTLIN_PARAMETER">fn</info>: <info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> () -> <info descr="null" textAttributesKey="KOTLIN_OBJECT">Unit</info>) {
    val <info descr="null" textAttributesKey="KOTLIN_LOCAL_VARIABLE">res</info>: <info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> (<info descr="null" textAttributesKey="KOTLIN_CLASS">Int</info>) -> <info descr="null" textAttributesKey="KOTLIN_CLASS">Int</info> = { <info descr="Automatically declared based on the expected type" textAttributesKey="KOTLIN_CLOSURE_DEFAULT_PARAMETER">it</info> + 1 };
    <info descr="null" textAttributesKey="KOTLIN_CONSTRUCTOR">T2</info>().<info descr="null" textAttributesKey="KOTLIN_FUNCTION_CALL">nonSuspend</info>()
    .<info descr="null" textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">suspend1</info>(<info descr="null" textAttributesKey="KOTLIN_PARAMETER">fn</info>)
    .<info descr="null" textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">suspend1</info> {  }
        .<info descr="null" textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">suspend1</info> { <info descr="null" textAttributesKey="KOTLIN_LOCAL_VARIABLE"><info descr="null" textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">res</info></info>(5) }
    <info descr="null" textAttributesKey="KOTLIN_LOCAL_VARIABLE"><info descr="null" textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">res</info></info>(5)
    <info descr="null" textAttributesKey="KOTLIN_PACKAGE_PROPERTY"><info descr="null" textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">fnType</info></info>()
    <info descr="null" textAttributesKey="KOTLIN_PACKAGE_PROPERTY"><info descr="null" textAttributesKey="KOTLIN_VARIABLE_AS_FUNCTION">fnFnType</info></info>().<info descr="null" textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">invoke</info>()
}
class <info descr="null" textAttributesKey="KOTLIN_CLASS">T2</info> {
    <info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> <info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">inline</info> fun <info descr="null" textAttributesKey="KOTLIN_FUNCTION_DECLARATION">suspend1</info>(<info descr="null" textAttributesKey="KOTLIN_PARAMETER">block</info>: <warning descr="[REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE] Redundant 'suspend' modifier: lambda parameters of suspend function type uses existing continuation." textAttributesKey="WARNING_ATTRIBUTES"><info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info></warning> () -> <info descr="null" textAttributesKey="KOTLIN_OBJECT">Unit</info>): <info descr="null" textAttributesKey="KOTLIN_CLASS">T2</info> {
        <info descr="null" textAttributesKey="KOTLIN_PARAMETER"><info descr="null" textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">block</info></info>()
        return this
    }
    fun <info descr="null" textAttributesKey="KOTLIN_FUNCTION_DECLARATION">nonSuspend</info>() = this
}

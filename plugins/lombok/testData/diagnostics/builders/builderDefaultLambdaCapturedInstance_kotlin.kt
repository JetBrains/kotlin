// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-90590
// WITH_STDLIB
import lombok.Builder
import kotlin.test.assertEquals

@Builder
class Entity {
    @Builder.Default
    var memberCall: Int = 42

    @Builder.Default
    // This error has to be here, otherwise we have a compilation crash instead:
    // [IR_VALIDATION_ERROR] ... references a value that is not available in the current scope.
    // GET_VAR '<this>: <root>.Entity' ... inside FUN_EXPR ... inside FUN build ... CLASS EntityBuilder
    var lambda: () -> Int = <!BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE!>{ memberCall }<!>
}

fun box(): String {
    val entity = Entity.builder().memberCall(7).build()
    assertEquals(7, entity.memberCall)
    // The default is evaluated by `build()`, where there is no `Entity` instance for the lambda to capture
    assertEquals(7, entity.lambda())
    return "OK"
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral */

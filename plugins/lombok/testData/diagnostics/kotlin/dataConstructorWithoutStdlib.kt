// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
import lombok.*

@<!MISSING_DEPENDENCY_CLASS("kotlin.jvm.JvmStatic")!>Data<!>(staticConstructor = "of")
class DataWithStatic(val name: String)

// Nothing is generated for the following declarations, so `MISSING_DEPENDENCY_CLASS` is not expected.
@Data(staticConstructor = "of")
object DataObjectWithStatic

@Data(staticConstructor = "of")
<!NO_ARGS_CONSTRUCTOR_FORCE_REQUIRED!>@NoArgsConstructor<!>
class DataWithNoArgsConstructor(val name: String)

/* GENERATED_FIR_TAGS: classDeclaration, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */

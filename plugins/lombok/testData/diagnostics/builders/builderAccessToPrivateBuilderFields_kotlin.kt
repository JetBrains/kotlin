import lombok.Builder

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class BuilderExample(val str: String, val id: Int)

fun test() {
    val builder = BuilderExample.<!UNRESOLVED_REFERENCE!>builder<!>()
    builder.str // Access to field despite the fact it's invisible
    builder.id // Access to field despite the fact it's invisible
}

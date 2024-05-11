import com.jetbrains.rhizomedb.*

<!WRONG_ANNOTATION_TARGET!>
@GeneratedEntityType<!>
typealias MyEntity = Entity

fun foo() {
    MyEntity.<!NONE_APPLICABLE!>all<!>()
    MyEntity.<!NONE_APPLICABLE!>single<!>()
    MyEntity.<!NONE_APPLICABLE!>singleOrNull<!>()
}

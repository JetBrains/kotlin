open class Base
open class OtherBase

class Some<T>
fun <TSome : Base, R : Base> Some<TSome>.extensionBase(param: R) = "Test"
fun <TSome : OtherBase, R : OtherBase> Some<TSome>.extensionOtherBase(param: R) = "Test"
fun <TSome> Some<TSome>.extensionExact() = "Test"

fun test() {
    Some<Base>().ex<caret>
}

// EXIST: extensionBase, extensionExact
// ABSENT: extensionOtherBase

// WITH_RUNTIME
// FIX: Convert to 'let'
class Employee(val firstName: String, val manager: Employee?)

fun test(employee: Employee) {
    val person = employee.also {
        it.manager?.<caret>run {
            println("${it.firstName} has a manager")
        }
    }
}
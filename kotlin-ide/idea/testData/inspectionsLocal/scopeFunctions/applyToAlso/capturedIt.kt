// WITH_RUNTIME
// FIX: Convert to 'also'
class Employee(val firstName: String, val manager: Employee?)

fun test(employee: Employee) {
    val person = employee.also {
        it.manager?.<caret>apply {
            println("${it.firstName} has a manager")
        }
    }
}
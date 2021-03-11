// INTENTION_TEXT: "Add import for 'pack.name.Fixtures'"

package pack.name

class Fixtures {
    class Register {
        class Domain {
            object UserRepository {
                val authSuccess = true
                val authError = false
            }
        }
    }
}

fun test() {
    pack.name.<caret>Fixtures.Register.Domain.UserRepository.authSuccess
}
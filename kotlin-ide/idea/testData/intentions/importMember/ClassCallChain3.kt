// INTENTION_TEXT: "Add import for 'pack.name.Fixtures.Register.Domain.UserRepository.authSuccess'"

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
    pack.name.Fixtures.Register.Domain.UserRepository.authSuccess<caret>
}
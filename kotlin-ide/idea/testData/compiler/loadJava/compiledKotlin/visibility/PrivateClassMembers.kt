//ALLOW_AST_ACCESS

package test

public class PrivateClassMembers {
    private val v = { 0 }()

    private var r = { 0 }()
        private set

    private fun f() = { 0 }()

    internal val internal = { 0 }()
}

import base.*

interface SubCheck : Check {
    override fun test(): String {
        return "OK"
    }
}

class <!EXPLICIT_OVERRIDE_REQUIRED_IN_MIXED_MODE!>SubCheckClass<!> : CheckClass(), SubCheck
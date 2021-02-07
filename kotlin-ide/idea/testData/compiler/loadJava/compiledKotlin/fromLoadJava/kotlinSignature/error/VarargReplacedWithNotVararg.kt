package test

import java.util.*

public open class VarargReplacedWithNotVararg {
    public open fun foo(vararg p0 : String?) {
        throw UnsupportedOperationException()
    }
}

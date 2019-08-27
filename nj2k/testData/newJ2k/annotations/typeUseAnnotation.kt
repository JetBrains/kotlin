// ERROR: This annotation is not applicable to target 'local variable'
import javaApi.TypeUseAnon1
import javaApi.TypeUseAnon2
import javaApi.TypeUseAnon3

class TEST1 {
    fun foo(o: @TypeUseAnon1 Any?): @TypeUseAnon1 String? {
        val baz = ""
        return ""
    }

    internal var bar: @TypeUseAnon1 String? = null
}

class TEST2 {
    fun foo(o: @TypeUseAnon2 Any?): @TypeUseAnon2 String? {
        val baz = ""
        return ""
    }

    internal var bar: @TypeUseAnon2 String? = null
}

class TEST3 {
    @TypeUseAnon3
    fun foo(@TypeUseAnon3 o: Any?): String {
        @TypeUseAnon3 val baz = ""
        return ""
    }

    @TypeUseAnon3
    internal var bar: String? = null
}
import javaApi.Anon5
import javaApi.TypeUseAnon1
import javaApi.TypeUseAnon2
import javaApi.TypeUseAnon3

class TEST1 {
    @Anon5(1)
    fun foo(@Anon5(2) o: @TypeUseAnon1 Any?): @TypeUseAnon1 String? {
        @Anon5(3) val baz = ""
        return ""
    }

    @Anon5(4)
    var bar: @TypeUseAnon1 String? = null
}

class TEST2 {
    @Anon5(1)
    fun foo(@Anon5(2) o: @TypeUseAnon2 Any?): @TypeUseAnon2 String? {
        @Anon5(3) val baz = ""
        return ""
    }

    @Anon5(4)
    var bar: @TypeUseAnon2 String? = null
}

class TEST3 {
    @Anon5(1)
    fun foo(@Anon5(2) o: @TypeUseAnon3 Any?): @TypeUseAnon3 String? {
        @Anon5(3) val baz = ""
        return ""
    }

    @Anon5(4)
    var bar: @TypeUseAnon3 String? = null
}
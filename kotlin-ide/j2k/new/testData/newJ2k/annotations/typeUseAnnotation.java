import javaApi.Anon5
import javaApi.TypeUseAnon1
import javaApi.TypeUseAnon2
import javaApi.TypeUseAnon3

public class TEST1 {
    public @Anon5(1) @TypeUseAnon1 String foo(@Anon5(2) @TypeUseAnon1 Object o) {
        @Anon5(3) @TypeUseAnon1 String baz = "";
        return "";
    }
    @Anon5(4) @TypeUseAnon1 String bar;
}

public class TEST2 {
    public @Anon5(1) @TypeUseAnon2 String foo(@Anon5(2) @TypeUseAnon2 Object o) {
        @Anon5(3) @TypeUseAnon2 String baz = "";
        return "";
    }
    @Anon5(4) @TypeUseAnon2 String bar;
}

public class TEST3 {
    public @Anon5(1) @TypeUseAnon3 String foo(@Anon5(2) @TypeUseAnon3 Object o) {
        @Anon5(3) @TypeUseAnon3 String baz = "";
        return "";
    }
    @Anon5(4) @TypeUseAnon3 String bar;
}
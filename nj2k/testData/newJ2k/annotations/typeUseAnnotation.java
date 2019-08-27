import javaApi.TypeUseAnon1
import javaApi.TypeUseAnon2
import javaApi.TypeUseAnon3

public class TEST1 {
    public @TypeUseAnon1 String foo(@TypeUseAnon1 Object o) {
        @TypeUseAnon1 String baz = "";
        return "";
    }
    @TypeUseAnon1 String bar;
}

public class TEST2 {
    public @TypeUseAnon2 String foo(@TypeUseAnon2 Object o) {
        @TypeUseAnon2 String baz = "";
        return "";
    }
    @TypeUseAnon2 String bar;
}


public class TEST3 {
    public @TypeUseAnon3 String foo(@TypeUseAnon3 Object o) {
        @TypeUseAnon3 String baz = "";
        return "";
    }
    @TypeUseAnon3 String bar;
}
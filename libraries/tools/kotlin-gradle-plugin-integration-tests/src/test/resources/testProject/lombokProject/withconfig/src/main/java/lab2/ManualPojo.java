package lab2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManualPojo {

    public String getFoo() {
        return null;
    }

//    @NotNull
    public String getBar() {
        return "234";
    }

    @Nullable
    public Object someMethod() {
        return null;
    }

}

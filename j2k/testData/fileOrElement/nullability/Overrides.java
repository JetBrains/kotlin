//file
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class Base {
    @Nullable
    public String foo(@Nullable String s) { return ""; }

    public String bar(String s) {
        return s != null ? s + 1 : null;
    }

    public String zoo(Object o){ return ""; }
}

interface I {
    @Nullable String zoo(@Nullable Object o);
}

class C extends Base implements I {
    public String foo(String s) { return ""; }

    public String bar(String s) { return ""; }

    public String zoo(Object o) { return ""; }
}

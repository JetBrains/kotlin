//file
import org.jetbrains.annotations.Nullable;

interface I {
    int getInt();
}

class C {
    @Nullable Object getObject() {
        foo(new I() {
            @Override
            public int getInt() {
                return 0;
            }
        });
        return string;
    }

    void foo(I i) {}

    @Nullable String string;
}
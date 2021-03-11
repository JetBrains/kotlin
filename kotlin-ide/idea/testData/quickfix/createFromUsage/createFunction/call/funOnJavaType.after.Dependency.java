import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class A {

    @Nullable
    public Integer foo(int i, @NotNull String s) {
        return null;
    }
}
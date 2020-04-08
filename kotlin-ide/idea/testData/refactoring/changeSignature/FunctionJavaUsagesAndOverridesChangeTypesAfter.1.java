import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class Y extends A {
    @Nullable
    @Override
    Object foo(@Nullable String n, int s, @Nullable Long o) {
        return "";
    }
}
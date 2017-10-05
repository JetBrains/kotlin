import org.jetbrains.annotations.NotNull;

public final class QueryFactory {
    @NotNull
    public final Query query() {
        return new Query();
    }
}

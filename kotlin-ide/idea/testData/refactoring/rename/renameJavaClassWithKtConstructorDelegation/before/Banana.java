import java.util.function.Supplier;

public class Banana {
    final Supplier<String> f;

    Banana(Supplier<String> f) {this.f = f;}

    Banana() {this(() -> "Default");}

    void goCrazy() {
        System.out.println(f.get());
    }
}
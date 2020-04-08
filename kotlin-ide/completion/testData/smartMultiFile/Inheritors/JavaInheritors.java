import p2.KotlinTrait

public abstract class JavaInheritor1 implements KotlinTrait {
}

public class JavaInheritor2 extends JavaInheritor1 {
    public JavaInheritor2() {
    }

    public JavaInheritor2(int p) {
    }
}

// not visible - it's package local
class JavaInheritor3 extends KotlinTrait {}


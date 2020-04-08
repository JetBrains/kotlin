import static foo.OverloadKt.overload;

public class OverloadJava {
    public void useOverload() {
        overload(0, false);
        overload(0, true);
        overload(0, true, 2.0);
    }
} 
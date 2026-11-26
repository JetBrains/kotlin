// ISSUE: KT-91284
// Review of the PR around KT-91284: Lombok never changes the visibility of an explicit constructor,
// even if a static factory with the same signature is generated next to it.
// `javac` + Lombok 1.18.48 + `javap` give:
//   DataWithExplicit:        public DataWithExplicit(int, java.lang.String); public static DataWithExplicit of(int, java.lang.String)
//   AllArgsNoneWithExplicit: public AllArgsNoneWithExplicit(int)  (AccessLevel.NONE: nothing is generated at all)
// Since commit "Lombok: hide explicit Java constructors if hidden by static factory" both explicit constructors
// are made private for Kotlin (`hidesExplicitConstructor` ignores both the declared visibility and `access`),
// so the calls below are reported as INVISIBLE_REFERENCE although they work at runtime.

// FILE: DataWithExplicit.java
import lombok.Data;

@Data(staticConstructor = "of")
public class DataWithExplicit {
    private final int a;
    private final String b;

    public DataWithExplicit(int a, String b) {
        this.a = a + 2;
        this.b = b + "_suffix";
    }

    static void javaUsage() {
        DataWithExplicit direct = new DataWithExplicit(1, "1");
        DataWithExplicit viaStaticConstructor = DataWithExplicit.of(1, "1");
    }
}

// FILE: AllArgsNoneWithExplicit.java
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(staticName = "of", access = AccessLevel.NONE)
public class AllArgsNoneWithExplicit {
    public int x;

    public AllArgsNoneWithExplicit(int x) {
        this.x = x;
    }

    static void javaUsage() {
        new AllArgsNoneWithExplicit(1);
    }
}

// FILE: test.kt
fun box(): String {
    DataWithExplicit.javaUsage()
    AllArgsNoneWithExplicit.javaUsage()

    val direct = DataWithExplicit(1, "1")
    if (direct != DataWithExplicit.of(1, "1")) return "Fail 1"
    if (direct.a != 3 || direct.b != "1_suffix") return "Fail 2"
    if (AllArgsNoneWithExplicit(1).x != 1) return "Fail 3"
    return "OK"
}

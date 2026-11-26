// ISSUE: KT-91284
// Review of the PR around KT-91284: the fix covers `@Data(staticConstructor = ...)` only (see `Data.asRequiredArgsConstructor`),
// while `@Value(staticConstructor = ...)` behaves the same way in Lombok: with an explicit constructor present,
// the static factory is still generated. `javac` + Lombok 1.18.48 + `javap` give:
//   public ValueWithExplicit(int); public static ValueWithExplicit of(int)
// `Value.asAllArgsConstructor` still returns null in presence of an explicit constructor, so `of` is unresolved.

// FILE: ValueWithExplicit.java
import lombok.Value;

@Value(staticConstructor = "of")
public class ValueWithExplicit {
    int a;

    public ValueWithExplicit(int a) {
        this.a = a + 1;
    }

    static void javaUsage() {
        ValueWithExplicit direct = new ValueWithExplicit(1);
        ValueWithExplicit viaStaticConstructor = ValueWithExplicit.of(1);
    }
}

// FILE: test.kt
fun box(): String {
    ValueWithExplicit.javaUsage()
    if (ValueWithExplicit.of(1) != ValueWithExplicit(1)) return "Fail"
    return "OK"
}

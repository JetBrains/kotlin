// SCRIPT_DEFAULT_IMPORTS: b.A

// MODULE: a
// FILE: a.java
package a;
public class A {
    public static void s() { return; }
}
// FILE: aa.java
package b;
public interface A {}
//MODULE: b(a)
// FILE: b.test.kts
import a.A
val b = A.s()

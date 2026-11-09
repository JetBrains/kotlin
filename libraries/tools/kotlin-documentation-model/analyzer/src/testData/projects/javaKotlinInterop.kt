// MODULE: main
// FILE: pkg/JPoint.java
package pkg;

public class JPoint {
    public int x;

    public int getX() {
        return x;
    }
}

// FILE: pkg/KUser.kt
package pkg

class KUser {
    fun describe(point: JPoint): Int = point.x
}

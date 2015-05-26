// ERROR: Val cannot be reassigned
// ERROR: Val cannot be reassigned
class A {
    private val s = ""
    private val x = 0

    public constructor() {
    }

    jvmOverloads public constructor(p: Int, s: String, x: Int = 1) {
        this.s = s
        this.x = x
    }
}
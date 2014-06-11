import java.io.*

public class C() {
    fun foo() {
        FileInputStream("foo").use { input ->
            FileOutputStream("bar").use { output ->
                output.write(input.read())
                output.write(0)
            }
        }
    }
}
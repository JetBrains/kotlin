import java.io.*

public class C {
    throws(javaClass<IOException>())
    fun foo() {
        FileInputStream("foo").use { input ->
            FileOutputStream("bar").use { output ->
                output.write(input.read())
                output.write(0)
            }
        }
    }
}
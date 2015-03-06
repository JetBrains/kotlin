// ERROR: None of the following functions can be called with the arguments supplied:  public constructor FileInputStream(p0: kotlin.String!) defined in java.io.FileInputStream public constructor FileInputStream(p0: java.io.File!) defined in java.io.FileInputStream public constructor FileInputStream(p0: [ERROR : Unresolved java classifier: FileDescriptor]!) defined in java.io.FileInputStream
// ERROR: None of the following functions can be called with the arguments supplied:  public constructor InputStreamReader(p0: java.io.InputStream!) defined in java.io.InputStreamReader public constructor InputStreamReader(p0: java.io.InputStream!, p1: kotlin.String!) defined in java.io.InputStreamReader public constructor InputStreamReader(p0: java.io.InputStream!, p1: java.nio.charset.Charset!) defined in java.io.InputStreamReader public constructor InputStreamReader(p0: java.io.InputStream!, p1: [ERROR : Unresolved java classifier: CharsetDecoder]!) defined in java.io.InputStreamReader
// ERROR: Assignments are not expressions, and only expressions are allowed in this context
// ERROR: Unresolved reference: close
import java.io.*

class FileRead {
    default object {
        public fun main(args: Array<String>) {
            try {
                val fstream = FileInputStream()
                val `in` = DataInputStream(fstream)
                val br = BufferedReader(InputStreamReader(`in`))
                val strLine: String
                while ((strLine = br.readLine()) != null) {
                    System.out.println(strLine)
                }
                `in`.close()
            } catch (e: Exception) {
                System.err.println("Error: " + e.getMessage())
            }

        }
    }
}

fun main(args: Array<String>) = FileRead.main(args)
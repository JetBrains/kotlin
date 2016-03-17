// ERROR: None of the following functions can be called with the arguments supplied:  public constructor FileInputStream(p0: File!) defined in java.io.FileInputStream public constructor FileInputStream(p0: FileDescriptor!) defined in java.io.FileInputStream public constructor FileInputStream(p0: String!) defined in java.io.FileInputStream
// ERROR: Type mismatch: inferred type is DataInputStream but InputStream! was expected
// ERROR: Assignments are not expressions, and only expressions are allowed in this context
// ERROR: Unresolved reference: close
import java.io.*

internal object FileRead {
    @JvmStatic fun main(args: Array<String>) {
        try {
            val fstream = FileInputStream()
            val `in` = DataInputStream(fstream)
            val br = BufferedReader(InputStreamReader(`in`))
            var strLine: String
            while ((strLine = br.readLine()) != null) {
                println(strLine)
            }
            `in`.close()
        } catch (e: Exception) {
            System.err.println("Error: " + e.message)
        }

    }
}
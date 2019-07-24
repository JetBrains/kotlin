// ERROR: Type mismatch: inferred type is DataInputStream but InputStream! was expected
// ERROR: Unresolved reference: close
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

internal object FileRead {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val fstream = FileInputStream(File("file.txt"))
            val `in` = DataInputStream(fstream)
            val br = BufferedReader(InputStreamReader(`in`))
            var strLine: String
            while (br.readLine().also { strLine = it } != null) {
                println(strLine)
            }
            `in`.close()
        } catch (e: Exception) {
            System.err.println("Error: " + e.message)
        }

    }
}

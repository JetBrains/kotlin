import java.io.*

class FileRead {
    class object {
        public fun main(args: Array<String>) {
            try {
                val fstream = FileInputStream("textfile.txt")
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
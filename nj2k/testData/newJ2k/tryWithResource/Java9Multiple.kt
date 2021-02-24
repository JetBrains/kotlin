import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class Java9 {
    @Throws(IOException::class)
    fun check() {
        val br = BufferedReader(InputStreamReader(System.`in`))
        val br2 = BufferedReader(InputStreamReader(System.`in`))
        br.use {
            br2.use {
                br.readLine()
                br2.readLine()
            }
        }
    }
}

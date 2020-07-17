import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class Java9 {
    @Throws(IOException::class)
    fun check() {
        val br = BufferedReader(InputStreamReader(System.`in`))
        br.use {
            BufferedReader(InputStreamReader(System.`in`)).use { br2 ->
                br.readLine()
                br2.readLine()
            }
        }
    }
}
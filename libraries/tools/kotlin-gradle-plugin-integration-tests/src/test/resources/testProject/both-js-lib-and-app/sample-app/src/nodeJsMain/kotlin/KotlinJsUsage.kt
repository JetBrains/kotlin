import com.example.lib.expectedFun
import com.example.lib.id
import com.example.lib.idUsage
import kotlin.js.Console

external val console: Console

fun nodeJsMain(args: Array<String>) {
    console.info(id(123), idUsage(), expectedFun())
}
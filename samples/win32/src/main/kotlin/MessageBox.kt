import kotlinx.cinterop.*
import platform.windows.*

fun main(args: Array<String>) {
    val message = StringBuilder()
    memScoped {
      val buffer = allocArray<UShortVar>(MAX_PATH)
      GetModuleFileNameW(null, buffer, MAX_PATH)
      val path = buffer.toKString().split("\\").dropLast(1).joinToString("\\")
      message.append("Я нахожусь в $path\n")
    }
    MessageBoxW(null, "Konan говорит:\nЗДРАВСТВУЙ МИР!\n$message",
            "Заголовок окна", (MB_YESNOCANCEL or MB_ICONQUESTION).convert())
}

import win32.*

fun main(args: Array<String>) {
    MessageBoxW(null, "Konan говорит:\nЗДРАВСТВУЙ МИР!\n",
            "Заголовок окна", MB_YESNOCANCEL or MB_ICONQUESTION)
}
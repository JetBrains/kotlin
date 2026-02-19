fun box(): String {
    var log = ""

    var i = 0
    `!`@while (true) {
        log += "i=$i;"
        for (j in 1..3) {
            log += "j=$j;"
            if (i++ + j > 5) break@`!`
            log += "!"
        }
    }

    if (log != "i=0;j=1;!j=2;!j=3;!i=3;j=1;!j=2;") return "fail: $log"

    return "OK"
}
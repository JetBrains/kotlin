import java.util.regex.Pattern

class State {
    def currentPlugin = ""
    def lines = []
}

def removePaths(String path, File basedir) {
    while (basedir.parentFile != null) {
        path = path.replace(basedir.absolutePath, "").replace(basedir.path, "")
        basedir = basedir.parentFile
    }

    return path
}

System.out.flush()
def buildLogFile = new File(basedir, "build.log")

def pattern = Pattern.compile(/\[INFO\] --- ([^:]+):.*:\S+ \([^)]+\) @ [a-zA-Z_-]+ ---/)
State state = buildLogFile.readLines().collect { it.replaceAll("\\u001b[^m]*m", "") }.inject(new State()) { acc, line ->
    def m = pattern.matcher(line)
    if (m.find()) {
        acc.currentPlugin = m.group(1)
    } else if (line.startsWith("[INFO] Downloaded:") ||
               line.startsWith("[INFO] Downloading:") ||
               line.startsWith("Downloaded:") ||
               line.startsWith("Downloading:") ||
               line.startsWith("[INFO] PERF:")) {
        // ignore line
    } else if (acc.currentPlugin == "kotlin-maven-plugin") {
        def filtered = removePaths(line, basedir).
                replace("\\", "/").
                replaceAll(/[0-9]+\s*ms/, "LLL ms").
                trim().
                replaceAll(/^\[[A-Z]+\]$/, "").
                replaceAll(/version [0-9a-z-+\.]+/, "version @snapshot@")

        if (filtered != "") {
            acc.lines << filtered
        }
    }

    acc
}

def expectedLog = new File(basedir, "expected.log").readLines().join("\n").trim()
def actualLog = state.lines.join("\n").trim()

if (expectedLog != actualLog) {
    println "Expected and actual log differs!"
    println ""

    println "Actual log (from $buildLogFile):"
    println actualLog
    println ""
    println "Expected log (from ${new File(basedir, "expected.log").absolutePath}):"
    println expectedLog
    println ""
    return false
} else {
    println "Log comparison succeeded"
}

return true

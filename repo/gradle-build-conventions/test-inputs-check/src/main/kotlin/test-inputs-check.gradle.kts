import jdk.jfr.consumer.RecordedFrame
import jdk.jfr.consumer.RecordingFile
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.nio.file.Paths
import kotlin.io.path.relativeTo

val disableInputsCheck = project.providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull?.toBoolean() == true

tasks.withType<Test>().configureEach {
    if (!disableInputsCheck) {
        val jfrFile = layout.buildDirectory.dir("jfr").get().asFile.resolve("test.jfr")
        val jfcFile = rootProject.file("gradle-file-read.jfc")
        val projectPath = projectDir.toPath()

        jvmArgs(
            "-XX:StartFlightRecording:" +
                    "settings=${jfcFile.absolutePath}," +
                    "disk=true," +
                    "dumponexit=true," +
                    "filename=${jfrFile.absolutePath}"
        )

        doFirst {
            jfrFile.parentFile.mkdirs()
        }

        doLast {
            val accessedFiles = RecordingFile.readAllEvents(jfrFile.toPath())
                .filter { it.eventType.name == "jdk.FileRead" }
                .filter { it.getString("path") != null }
                .map {
                    AccessedFile(
                        path = Paths.get(it.getString("path")!!),
                        stacktrace = it.stackTrace.frames
                    )
                }
                .map {
                    AccessedFile(
                        path = Paths.get((if (!it.path.isAbsolute) projectPath.resolve(it.path) else it.path).toFile().canonicalPath),
                        stacktrace = it.stacktrace
                    )
                }
                .filter { it.path.startsWith(projectPath) }
                .associateBy { it.path }

            val accessedPaths = accessedFiles.keys
            val declaredInputs = inputs.files.asFileTree.map { it.toPath() }.toSet()
            val undeclaredInputs = accessedPaths - declaredInputs
            val undeclaredInputFiles = undeclaredInputs.map { accessedFiles[it] }

            if (undeclaredInputFiles.isNotEmpty()) {
                val reportFile = layout.buildDirectory.file("undeclared-inputs.html").get().asFile
                reportFile.parentFile.mkdirs()
                reportFile.writeText(buildHtmlReport(undeclaredInputFiles.filterNotNull()))

                error(buildString {
                    appendLine("Undeclared inputs found! (${undeclaredInputFiles.size})")
                    appendLine("Report: ${reportFile.toURI()}")
                })
            }
        }
    }
}

data class AccessedFile(
    val path: java.nio.file.Path,
    val stacktrace: List<RecordedFrame>,
) {
    fun formatStacktrace() = buildString {
        stacktrace.forEach { frame ->
            val method = frame.method
            val className = method.type.name
            val methodName = method.name
            val lineNumber = frame.lineNumber
            append("    at $className.$methodName(")
            if (lineNumber >= 0) {
                append("${method.type.name.substringAfterLast('.')}.java:$lineNumber")
            } else {
                append("Unknown Source")
            }
            appendLine(")")
        }
    }

    override fun toString() = buildString {
        appendLine("path = $path")
        append(formatStacktrace())
    }
}

private fun jsonString(s: String): String = buildString {
    append('"')
    for (c in s) {
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '<' -> append("\\u003c")
            '>' -> append("\\u003e")
            '&' -> append("\\u0026")
            '\u2028' -> append("\\u2028")
            '\u2029' -> append("\\u2029")
            else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
        }
    }
    append('"')
}

fun buildHtmlReport(files: List<AccessedFile>): String {
    val pageSize = 1000
    val totalPages = (files.size + pageSize - 1) / pageSize
    val projectPath = projectDir.toPath()

    val stackIndex = HashMap<String, Int>()
    val stacks = ArrayList<String>()
    val itemsJson = files.joinToString(separator = ",", prefix = "[", postfix = "]") { f ->
        val stack = f.formatStacktrace()
        val idx = stackIndex.getOrPut(stack) { stacks.add(stack); stacks.size - 1 }
        "{\"p\":${jsonString(f.path.relativeTo(projectPath).toString())},\"s\":$idx}"
    }
    val stacksJson = stacks.joinToString(separator = ",", prefix = "[", postfix = "]") { jsonString(it) }
    val dataJson = "{\"items\":$itemsJson,\"stacks\":$stacksJson}"

    return createHTML().html {
        attributes["lang"] = "en"
        head {
            meta(charset = "UTF-8")
            title("Undeclared inputs (${files.size})")
            style {
                unsafe {
                    +"""
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; margin: 20px; }
                        h1 { font-size: 18px; }
                        details { border: 1px solid #ddd; border-radius: 4px; margin: 4px 0; padding: 6px 10px; }
                        details > summary { cursor: pointer; font-family: 'SF Mono', Menlo, monospace; font-size: 13px; }
                        pre { background: #f6f8fa; padding: 10px; border-radius: 4px; overflow-x: auto; font-size: 12px; margin: 8px 0 0 0; white-space: pre-wrap; }
                        .pager { margin: 12px 0; display: flex; gap: 8px; align-items: center; }
                        .pager button { padding: 4px 10px; cursor: pointer; }
                        .pager button:disabled { cursor: default; opacity: 0.5; }
                    """.trimIndent()
                }
            }
        }
        body {
            h1 { +"Undeclared inputs found (${files.size})" }
            div("pager") {
                button { id = "prev"; unsafe { +"&laquo; Prev" } }
                span {
                    +"Page "
                    span { id = "pageNum"; +"1" }
                    +" of $totalPages"
                }
                button { id = "next"; unsafe { +"Next &raquo;" } }
            }
            div { id = "items" }
            script {
                unsafe {
                    +"""
                        (function() {
                          var DATA = $dataJson;
                          var ITEMS = DATA.items;
                          var STACKS = DATA.stacks;
                          var PAGE_SIZE = $pageSize;
                          var totalPages = $totalPages;
                          var current = 0;
                          var container = document.getElementById('items');
                          var prev = document.getElementById('prev');
                          var next = document.getElementById('next');
                          var label = document.getElementById('pageNum');
                          function render() {
                            var frag = document.createDocumentFragment();
                            var start = current * PAGE_SIZE;
                            var end = Math.min(start + PAGE_SIZE, ITEMS.length);
                            for (var i = start; i < end; i++) {
                              var item = ITEMS[i];
                              var d = document.createElement('details');
                              var s = document.createElement('summary');
                              s.textContent = item.p;
                              var p = document.createElement('pre');
                              p.textContent = STACKS[item.s];
                              d.appendChild(s);
                              d.appendChild(p);
                              frag.appendChild(d);
                            }
                            container.replaceChildren(frag);
                            label.textContent = current + 1;
                            prev.disabled = current === 0;
                            next.disabled = current >= totalPages - 1;
                            window.scrollTo(0, 0);
                          }
                          prev.addEventListener('click', function() { if (current > 0) { current--; render(); } });
                          next.addEventListener('click', function() { if (current < totalPages - 1) { current++; render(); } });
                          render();
                        })();
                    """.trimIndent()
                }
            }
        }
    }
}

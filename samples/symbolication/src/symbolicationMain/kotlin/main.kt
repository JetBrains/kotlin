package symbolication

import platform.darwin.*

import kotlinx.cinterop.*
import CoreSymbolication.*

fun main(args: Array<String>) {
    val program = if (args.size > 0 && args[0].isNotEmpty())
        args[0] else throw Error("please specify .dSYM path as the first argument")
    val arch = if (args.size > 1 && args[1].isNotEmpty()) args[1] else "current"
    val input = generateSequence { readLine() }
    analyzeTrace(program, arch, input)
}

fun archToCpuName(arch: String): cpu_type_t = when (arch) {
    "x64", "x86_64" -> CPU_TYPE_X86_64
    "arm32" -> CPU_TYPE_ARM
    "arm64" -> CPU_TYPE_ARM64
    "current" -> CSArchitectureGetCurrent()
    else -> TODO("unsupported $arch")
}

fun analyzeTrace(program: String, arch: String, input: Sequence<String>) {
    val symbolicator = CSSymbolicatorCreateWithPathAndArchitecture(program, archToCpuName(arch))
    if (CSIsNull(symbolicator)) throw Error("Cannot create \"$arch\" symbolicator for $program")
    val owner = CSSymbolicatorGetSymbolOwner(symbolicator)
    // Format is like
    // at 14  test.kexe                           0x0000000106a2e071 Konan_run_start + 113
    val matcher = "(at \\d+\\s+\\S+\\s+0x[0-9a-f]+\\s+)([^+]+)\\+\\s(\\d+)".toRegex()
    for (line in input) {
        val match = matcher.find(line)
        var result = line
        if (match != null) {
            val atPart =  match.groupValues[1]
            val functionName = match.groupValues[2].trim()
            val offsetInFunction =  match.groupValues[3].toUInt()
            val symbol = CSSymbolicatorGetSymbolWithNameAtTime(symbolicator, functionName, kCSNow)
            if (!CSIsNull(symbol)) {
                CSSymbolGetRange(symbol).useContents {
                    val address = this.location + offsetInFunction
                    val sourceInfo = CSSymbolOwnerGetSourceInfoWithAddress(owner, address)
                    if (!CSIsNull(sourceInfo)) {
                        val filePath = CSSourceInfoGetPath(sourceInfo)?.toKString()
                        // or val fileName = CSSourceInfoGetFilename(sourceInfo)?.toKString()
                        val lineNumber = CSSourceInfoGetLineNumber(sourceInfo)
                        val lineNumberString = if (lineNumber != 0u) lineNumber.toString() else "<unknown>"
                        if (filePath != null)
                            result = matcher.replaceFirst(line,
                                    "$atPart$functionName $filePath:$lineNumberString")
                    }
                }
            }
        }
        println(result)
    }
    CSRelease(symbolicator)
}

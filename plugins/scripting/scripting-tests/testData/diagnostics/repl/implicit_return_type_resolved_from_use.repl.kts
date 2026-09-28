// SNIPPET

enum class Build { Debug, Release }

// SNIPPET

fun applySomething(build: Build) = when (build) {
    Build.Debug -> "OK"
    Build.Release -> "fail"
}

fun isDebug(build: Build) = if (build == Build.Debug) "OK" else "fail"

val res = applySomething(Build.Debug) + isDebug(Build.Debug)

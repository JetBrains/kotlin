// FREE_COMPILER_ARGS: -Xruntime-logs=gc=info,mm=warning,tls=error,logging=debug
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// OUTPUT_REGEX: \[INFO\]\[logging\].*Logging enabled for: logging = DEBUG, gc = INFO, mm = WARNING, tls = ERROR.*\[INFO\]\[gc\].*
fun main() {}
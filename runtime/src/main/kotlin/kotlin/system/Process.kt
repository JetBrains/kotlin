package kotlin.system

/**
 * Terminates the currently running process.
 * The argument serves as a status code; by convention,
 * a nonzero status code indicates abnormal termination.
 *
 * This method never returns normally.
 */
@SymbolName("Kotlin_system_exitProcess")
public external fun exitProcess(status: Int): Nothing
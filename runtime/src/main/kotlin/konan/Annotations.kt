package konan

/**
 * Forces the compiler to use specified symbol name for the target `external` function.
 *
 * TODO: changing symbol name breaks the binary compatibility,
 * so it should probably be allowed on `internal` and `private` functions only.
 */
//@Target(AnnotationTarget.FUNCTION)
//@Retention(AnnotationRetention.SOURCE)
annotation class SymbolName(val name: String)

/**
 * Exports the TypeInfo of this class by given name to use it from runtime.
 */
//@Target(AnnotationTarget.CLASS)
//@Retention(AnnotationRetention.SOURCE)
annotation class ExportTypeInfo(val name: String)


/**
 * Preserve the function entry point during global optimizations
 */
public annotation class Used


// Following annotations can be used to mark functions that need to be fixed,
// once certain language feature is implemented.
/**
 * Need to be fixed because of boxing.
 */
public annotation class FixmeBoxing

/**
 * Need to be fixed because of inner classes.
 */
public annotation class FixmeInner

/**
 * Need to be fixed because of lambdas.
 */
public annotation class FixmeLambda

/**
 * Need to be fixed.
 */
public annotation class Fixme

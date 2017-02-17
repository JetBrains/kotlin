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


/**
 * Need to be fixed because of reification support.
 */
public annotation class FixmeReified

/**
 * Need to be fixed because of sorting support.
 */
public annotation class FixmeSorting

/**
 * Need to be fixed because of specialization support.
 */
public annotation class FixmeSpecialization

/**
 * Need to be fixed because of sequences support.
 */
public annotation class FixmeSequences

/**
 * Need to be fixed because of variance support.
 */
public annotation class FixmeVariance

/**
 * Need to be fixed because of regular expressions.
 */
public annotation class FixmeRegex

/**
 * Need to be fixed.
 */
public annotation class Fixme

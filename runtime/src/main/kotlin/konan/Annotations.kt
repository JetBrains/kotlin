/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * Need to be fixed because of reflection.
 */
public annotation class FixmeReflection

/**
 * Need to be fixed because of concurrency.
 */
public annotation class FixmeConcurrency

public annotation class FixmeInline

/**
 * Need to be fixed.
 */
public annotation class Fixme

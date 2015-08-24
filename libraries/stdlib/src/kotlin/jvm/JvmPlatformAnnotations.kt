/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.jvm

/**
 * Instructs the Kotlin compiler to generate overloads for this function that substitute default parameter values.
 *
 * If a method has N parameters and M of which have default values, M overloads are generated: the first one
 * takes N-1 parameters (all but the last one that takes a default value), the second takes N-2 parameters, and so on.
 */
target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
public annotation(retention = AnnotationRetention.BINARY, mustBeDocumented = true) class jvmOverloads


/**
 * Specifies that a static method or field needs to be generated from this element.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/java-interop.html#static-methods-and-fields)
 * for more information.
 */
target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
public annotation(retention = AnnotationRetention.RUNTIME, mustBeDocumented = true) class jvmStatic

/**
 * Specifies the name for the Java class or method
 * which is generated from this element.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/java-interop.html#handling-signature-clashes-with-platformname)
 * for more information.
 * @property name the name of the element.
 */
target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
public annotation(retention = AnnotationRetention.RUNTIME, mustBeDocumented = true) class jvmName(public val name: String)

/**
 * Instructs the Kotlin compiler to generate a public backing field for this property.
 */
target(AnnotationTarget.PROPERTY)
public annotation(retention = AnnotationRetention.SOURCE, mustBeDocumented = true) class publicField
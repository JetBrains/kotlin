/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.di

public fun generator(
        targetSourceRoot: String,
        injectorPackageName: String,
        injectorClassName: String,
        generatedBy: String,
        body: DependencyInjectorGenerator.() -> Unit
): DependencyInjectorGenerator {
    val generator = DependencyInjectorGenerator()
    generator.configure(targetSourceRoot, injectorPackageName, injectorClassName, generatedBy)
    generator.body()
    return generator
}

public fun DependencyInjectorGenerator.field(
        fieldType: Class<*>,
        name: String = defaultName(fieldType),
        init: Expression? = null,
        useAsContext: Boolean = false
) {
    addField(false, DiType(fieldType), name, init, useAsContext)
}

public fun DependencyInjectorGenerator.publicField(
        fieldType: Class<*>,
        name: String = defaultName(fieldType),
        init: Expression? = null,
        useAsContext: Boolean = false
) {
    addField(true, DiType(fieldType), name, init, useAsContext)
}

public fun DependencyInjectorGenerator.fields(vararg types: Class<*>): Unit = types.forEach { field(it) }
public fun DependencyInjectorGenerator.publicFields(vararg types: Class<*>): Unit = types.forEach { publicField(it) }

public fun DependencyInjectorGenerator.parameter(
        parameterType: Class<*>,
        name: String = defaultName(parameterType),
        useAsContext: Boolean = false
) {
    addParameter(false, DiType(parameterType), name, true, useAsContext)
}

public fun DependencyInjectorGenerator.publicParameter(
        parameterType: Class<*>,
        name: String = defaultName(parameterType),
        useAsContext: Boolean = false
) {
    addParameter(true, DiType(parameterType), name, true, useAsContext)
}

public fun DependencyInjectorGenerator.parameters(vararg types: Class<*>): Unit = types.forEach { parameter(it) }
public fun DependencyInjectorGenerator.publicParameters(vararg types: Class<*>): Unit = types.forEach { publicParameter(it) }

private fun defaultName(entityType: Class<*>) = InjectorGeneratorUtil.`var`(DiType(entityType))
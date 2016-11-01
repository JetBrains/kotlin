/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.kapt3.util

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal fun isEnum(access: Int) = (access and Opcodes.ACC_ENUM) != 0
internal fun isPublic(access: Int) = (access and Opcodes.ACC_PUBLIC) != 0
internal fun isSynthetic(access: Int) = (access and Opcodes.ACC_SYNTHETIC) != 0
internal fun isFinal(access: Int) = (access and Opcodes.ACC_FINAL) != 0
internal fun isStatic(access: Int) = (access and Opcodes.ACC_STATIC) != 0
internal fun isAbstract(access: Int) = (access and Opcodes.ACC_ABSTRACT) != 0
internal fun ClassNode.isEnum() = (access and Opcodes.ACC_ENUM) != 0
internal fun ClassNode.isAnnotation() = (access and Opcodes.ACC_ANNOTATION) != 0
internal fun MethodNode.isVarargs() = (access and Opcodes.ACC_VARARGS) != 0

internal fun <T> List<T>?.isNullOrEmpty() = this == null || this.isEmpty()
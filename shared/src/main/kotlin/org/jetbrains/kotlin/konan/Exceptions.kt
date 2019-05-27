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

package org.jetbrains.kotlin.konan

/**
 * This is a common ancestor of all Kotlin/Native exceptions.
 */
open class KonanException(message: String = "", cause: Throwable? = null) : Exception(message, cause)

/**
 * An error occured during external tool invocation. Such as non-zero exit code. 
 */
class KonanExternalToolFailure(message: String, val toolName: String, cause: Throwable? = null) : KonanException(message, cause)


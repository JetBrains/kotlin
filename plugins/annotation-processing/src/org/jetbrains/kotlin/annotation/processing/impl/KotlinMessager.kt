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

package org.jetbrains.kotlin.annotation.processing.impl

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import javax.annotation.processing.Messager
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind

class KotlinMessager(private val messageCollector: MessageCollector) : Messager {
    var errorCount: Int = 0
        private set
    
    var warningCount: Int = 0
        private set

    override fun printMessage(kind: Diagnostic.Kind, msg: CharSequence) = printMessage(kind, msg, null)

    override fun printMessage(kind: Diagnostic.Kind, msg: CharSequence, e: Element?) = printMessage(kind, msg, e, null)

    override fun printMessage(kind: Diagnostic.Kind, msg: CharSequence, e: Element?, a: AnnotationMirror?) {
        printMessage(kind, msg, e, a, null)
    }

    override fun printMessage(kind: Diagnostic.Kind, msg: CharSequence, e: Element?, a: AnnotationMirror?, v: AnnotationValue?) {
        val severity = when (kind) {
            Kind.ERROR -> {
                errorCount++
                CompilerMessageSeverity.ERROR
            }
            Kind.WARNING, Kind.MANDATORY_WARNING -> {
                warningCount++
                CompilerMessageSeverity.WARNING
            }
            else -> CompilerMessageSeverity.LOGGING
        }
        messageCollector.report(severity, msg.toString(), CompilerMessageLocation.NO_LOCATION)
    }
}
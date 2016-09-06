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

import java.io.File
import javax.tools.FileObject
import javax.tools.JavaFileObject

class KotlinJavaFileObject(file: File, fileName: String) : KotlinAbstractFileObject(file, fileName), JavaFileObject {
    //TODO
    override fun isNameCompatible(simpleName: String, kind: JavaFileObject.Kind) = true

    override fun getKind() = when (file.extension) {
        "class" -> JavaFileObject.Kind.CLASS
        "java" -> JavaFileObject.Kind.SOURCE
        "html" -> JavaFileObject.Kind.HTML
        else -> JavaFileObject.Kind.OTHER
    }

    //TODO
    override fun getAccessLevel() = null

    //TODO
    override fun getNestingKind() = null
}

class KotlinFileObject(file: File, fileName: String) : KotlinAbstractFileObject(file, fileName)

abstract class KotlinAbstractFileObject(val file: File, val fileName: String) : FileObject {
    override fun openOutputStream() = file.outputStream()

    override fun getName() = fileName

    override fun openWriter() = file.writer()

    override fun openInputStream() = file.inputStream()

    override fun getCharContent(ignoreEncodingErrors: Boolean) = file.readText()

    override fun getLastModified() = file.lastModified()

    override fun toUri() = file.toURI()

    override fun openReader(ignoreEncodingErrors: Boolean) = file.reader()

    override fun delete() = file.delete()
}
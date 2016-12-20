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

package org.jetbrains.kotlin.jps

import org.jetbrains.jps.javac.OptimizedFileManagerUtil
import java.lang.reflect.Field
import java.lang.reflect.Modifier

fun disableJava6FileManager() {
    val fileManagerClass = OptimizedFileManagerUtil.getManagerClass()
    if (fileManagerClass?.simpleName == "OptimizedFileManager") {
        // JPS tests depends on idea.jar and can't be executed under Java 1.6 anymore. But currently TeamCity merges classpath from all
        // dependencies in a one big mess with no differences between JDK and non-JDK jars. Such behaviour causes both
        // JDK 8 and JDK 6 classes present in classpath. Next there's ClasspathBootstrap.OptimizedFileManagerClassHolder in idea
        // that works through reflection and tries to choose correct FileManager that are not the same in JDK 6 and JDK 8. Choosing
        // FileManager for JDK 6 leads to exceptions on Runtime as classes from JDK 8 goes first on teamcity.
        //
        // Here we disable JDK 6 FileManager with brute force.
        val clazz = Class.forName("org.jetbrains.jps.cmdline.ClasspathBootstrap\$OptimizedFileManagerClassHolder")
        setFinalStaticToNull(clazz.getDeclaredField("managerClass"))
        setFinalStaticToNull(clazz.getDeclaredField("directoryCacheClearMethod"))
    }
}

private fun setFinalStaticToNull(field: Field) {
    field.isAccessible = true

    val modifiersField = (Field::class.java).getDeclaredField("modifiers")
    modifiersField.isAccessible = true
    modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

    field.set(null, null)
}

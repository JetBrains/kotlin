package org.jetbrains.kotlin.kapt3.javac

import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.main.Option
import com.sun.tools.javac.util.Context
import javax.tools.JavaFileManager

class KaptJavaFileManager(context: Context) : JavacFileManager(context, true, null) {
    fun handleOptionJavac9(option: Option, value: String) {
        val handleOptionMethod = JavacFileManager::class.java
            .getMethod("handleOption", Option::class.java, String::class.java)

        handleOptionMethod.invoke(this, option, value)
    }

    companion object {
        internal fun preRegister(context: Context) {
            context.put(JavaFileManager::class.java, Context.Factory<JavaFileManager> { KaptJavaFileManager(it) })
        }
    }
}
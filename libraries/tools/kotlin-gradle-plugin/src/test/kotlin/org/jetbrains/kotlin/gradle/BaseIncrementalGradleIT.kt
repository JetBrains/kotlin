package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import java.io.File
import java.util.*


abstract class BaseIncrementalGradleIT : BaseGradleIT() {

    val jpsResourcesPath = File("../../../jps-plugin/testData/incremental")

    open inner class IncrementalTestProject(name: String, wrapperVersion: String = "1.6", minLogLevel: LogLevel = LogLevel.DEBUG) : Project(name, wrapperVersion, minLogLevel) {
        var modificationStage: Int = 1
    }

    inner class JpsTestProject(val relPath: String, wrapperVersion: String = "1.6", minLogLevel: LogLevel = LogLevel.DEBUG) : IncrementalTestProject(File(relPath).name, wrapperVersion, minLogLevel) {
        override val resourcesRoot = File(jpsResourcesPath, relPath)

        override fun setupWorkingDir() {
            super.setupWorkingDir()
            File(projectDir, "build.gradle").writeText("""
buildscript {
  repositories {
    maven {
        url 'file://' + pathToKotlinPlugin
    }
  }
  dependencies {
    classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:0.1-SNAPSHOT'
  }
}

apply plugin: "kotlin"

sourceSets {
  main {
     kotlin {
        srcDir '.'
     }
     java {
        srcDir '.'
     }
  }
  test {
     kotlin {
        srcDir '.'
     }
     java {
        srcDir '.'
     }
  }
}

repositories {
  maven {
     url 'file://' + pathToKotlinPlugin
  }
}
            """)
        }
    }

    fun IncrementalTestProject.modify(runStage: Int? = null) {
        val projectDir = File(workingDir, projectName)
        assert(projectDir.exists())
        val actualStage = runStage ?: modificationStage

        fun String.toIntOr(defaultVal: Int): Pair<Int, Boolean> {
            try {
                return Pair(toInt(), true)
            }
            catch (e: NumberFormatException) {
                return Pair(defaultVal, false)
            }
        }

        projectDir.walk().filter { it.isFile }.forEach {
            val nameParts = it.name.split(".")
            if (nameParts.size > 2) {
                val (fileStage, hasStage) = nameParts.last().toIntOr(0)
                if (!hasStage || fileStage == actualStage) {
                    val orig = File(it.parent, nameParts.dropLast(if (hasStage) 2 else 1).joinToString("."))
                    when (if (hasStage) nameParts.get(nameParts.size - 2) else nameParts.last()) {
                        "touch" -> {
                            assert(orig.exists())
                            orig.setLastModified(Date().time)
                        }
                        "new" -> {
                            it.setLastModified(Date().time)
                            if (orig.exists()) {
                                orig.delete()
                            }
                            it.renameTo(orig)
                        }
                        "delete" -> {
                            assert(orig.exists())
                            orig.delete()
                        }
                    }
                }
            }
        }

        modificationStage = actualStage + 1
    }

}


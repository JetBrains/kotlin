package org.jetbrains.kotlin.jps.build

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.jps.JpsKotlinCompilerSettings
import kotlin.reflect.KMutableProperty1

class KotlinJpsBuildTestIncremental : KotlinJpsBuildTest() {
    var isICEnabledBackup: Boolean = false

    override fun setUp() {
        super.setUp()
        isICEnabledBackup = IncrementalCompilation.isEnabled()
        IncrementalCompilation.setIsEnabled(true)
    }

    override fun tearDown() {
        IncrementalCompilation.setIsEnabled(isICEnabledBackup)
        super.tearDown()
    }

    fun testManyFiles() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), arrayOf("src/Bar.kt"), arrayOf(klass("kotlinProject", "foo.Bar")))

        checkWhen(del("src/main.kt"),
                  pathsToCompile = null,
                  pathsToDelete = packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")
        assertFilesNotExistInOutput(module, "foo/MainKt.class")

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), null, arrayOf(klass("kotlinProject", "foo.Bar")))
    }

    fun testManyFilesForPackage() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"),
                  arrayOf("src/Bar.kt"),
                  arrayOf(klass("kotlinProject", "foo.Bar"),
                          packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                          module("kotlinProject")))

        checkWhen(del("src/main.kt"),
                  pathsToCompile = null,
                  pathsToDelete = packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), null,
                  arrayOf(klass("kotlinProject", "foo.Bar"),
                          packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                          module("kotlinProject")))
    }

    @WorkingDir("LanguageOrApiVersionChanged")
    fun testLanguageVersionChanged() {
        languageOrApiVersionChanged(CommonCompilerArguments::languageVersion)
    }

    @WorkingDir("LanguageOrApiVersionChanged")
    fun testApiVersionChanged() {
        languageOrApiVersionChanged(CommonCompilerArguments::apiVersion)
    }

    private fun languageOrApiVersionChanged(versionProperty: KMutableProperty1<CommonCompilerArguments, String?>) {
        initProject(LibraryDependency.JVM_MOCK_RUNTIME)

        assertEquals(1, myProject.modules.size)
        val module = myProject.modules.first()
        val args = JpsKotlinCompilerSettings.getCommonCompilerArguments(module)

        fun setVersion(newVersion: String) {
            versionProperty.set(args, newVersion)
            JpsKotlinCompilerSettings.setCommonCompilerArguments(myProject, args)
        }

        assertNull(args.apiVersion)
        buildAllModules().assertSuccessful()

        setVersion(LanguageVersion.LATEST_STABLE.versionString)
        buildAllModules().assertSuccessful()
        assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME)

        setVersion(LanguageVersion.KOTLIN_1_0.versionString)
        buildAllModules().assertSuccessful()
        assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/Bar.kt", "src/Foo.kt")
    }
}
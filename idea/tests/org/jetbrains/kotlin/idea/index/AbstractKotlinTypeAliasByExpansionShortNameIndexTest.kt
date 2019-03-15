/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.index

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasByExpansionShortNameIndex
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import kotlin.reflect.KMutableProperty0

abstract class AbstractKotlinTypeAliasByExpansionShortNameIndexTest : KotlinLightCodeInsightFixtureTestCase() {


    override fun getTestDataPath(): String {
        return KotlinTestUtils.getHomeDirectory() + "/"
    }

    private lateinit var scope: GlobalSearchScope

    override fun setUp() {
        super.setUp()
        scope = GlobalSearchScope.allScope(project)
    }

    override fun tearDown() {
        (this::scope as KMutableProperty0<GlobalSearchScope?>).set(null)
        super.tearDown()
    }

    override fun getProjectDescriptor() = super.getProjectDescriptorFromTestName()

    fun doTest(file: String) {
        myFixture.configureByFile(file)
        val fileText = myFixture.file.text
        InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "CONTAINS").forEach {
            assertIndexContains(it)
        }
    }

    private val regex = "\\(key=\"(.*?)\"[, ]*value=\"(.*?)\"\\)".toRegex()

    fun assertIndexContains(record: String) {
        val index = KotlinTypeAliasByExpansionShortNameIndex.INSTANCE
        val (_, key, value) = regex.find(record)!!.groupValues
        val result = index.get(key, project, scope)
        if (value !in result.map { it.name }) {
            Assert.fail(buildString {
                appendln("Record $record not found in index")
                appendln("Index contents:")
                index.getAllKeys(project).asSequence().forEach {
                    appendln("KEY: $it")
                    index.get(it, project, scope).forEach {
                        appendln("    ${it.name}")
                    }
                }
            })
        }
    }

}
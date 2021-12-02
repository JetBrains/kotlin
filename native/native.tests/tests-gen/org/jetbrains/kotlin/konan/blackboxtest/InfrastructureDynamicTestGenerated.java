/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.junit.jupiter.api.Tag;
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UseStandardTestCaseGroupProvider;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public class InfrastructureDynamicTestGenerated extends AbstractNativeBlackBoxTest {
    @Nested
    @TestMetadata("native/native.tests/testData/samples")
    @TestDataPath("$PROJECT_ROOT")
    @Tag("infrastructure")
    @UseStandardTestCaseGroupProvider()
    public class Samples {
        @Test
        public void testAllFilesPresentInSamples() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("native/native.tests/testData/samples"), Pattern.compile("^(.+)\\.kt$"), null, true);
        }

        @TestFactory
        @TestMetadata("regular_custom_args.kt")
        public Object testRegular_custom_args() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_custom_args.kt");
        }

        @TestFactory
        @TestMetadata("regular_multifile.kt")
        public Object testRegular_multifile() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_multifile.kt");
        }

        @TestFactory
        @TestMetadata("regular_multifile_with_explicit_packages.kt")
        public Object testRegular_multifile_with_explicit_packages() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_multifile_with_explicit_packages.kt");
        }

        @TestFactory
        @TestMetadata("regular_multimodule.kt")
        public Object testRegular_multimodule() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_multimodule.kt");
        }

        @TestFactory
        @TestMetadata("regular_multimodule_implicit_first_module.kt")
        public Object testRegular_multimodule_implicit_first_module() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_multimodule_implicit_first_module.kt");
        }

        @TestFactory
        @TestMetadata("regular_multimodule_implicit_first_module_with_header_comment.kt")
        public Object testRegular_multimodule_implicit_first_module_with_header_comment() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_multimodule_implicit_first_module_with_header_comment.kt");
        }

        @TestFactory
        @TestMetadata("regular_multimodule_implicit_first_module_with_header_statement.kt")
        public Object testRegular_multimodule_implicit_first_module_with_header_statement() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_multimodule_implicit_first_module_with_header_statement.kt");
        }

        @TestFactory
        @TestMetadata("regular_multimodule_with_header_comment.kt")
        public Object testRegular_multimodule_with_header_comment() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_multimodule_with_header_comment.kt");
        }

        @TestFactory
        @TestMetadata("regular_multimodule_with_header_statement.kt")
        public Object testRegular_multimodule_with_header_statement() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_multimodule_with_header_statement.kt");
        }

        @TestFactory
        @TestMetadata("regular_simple.kt")
        public Object testRegular_simple() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_simple.kt");
        }

        @TestFactory
        @TestMetadata("regular_simple_default_tr.kt")
        public Object testRegular_simple_default_tr() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_simple_default_tr.kt");
        }

        @TestFactory
        @TestMetadata("regular_simple_explicit_kind.kt")
        public Object testRegular_simple_explicit_kind() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_simple_explicit_kind.kt");
        }

        @TestFactory
        @TestMetadata("regular_simple_noexit_tr.kt")
        public Object testRegular_simple_noexit_tr() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_simple_noexit_tr.kt");
        }

        @TestFactory
        @TestMetadata("regular_simple_with_output.kt")
        public Object testRegular_simple_with_output() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_simple_with_output.kt");
        }

        @TestFactory
        @TestMetadata("regular_simple_worker_tr.kt")
        public Object testRegular_simple_worker_tr() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/regular_simple_worker_tr.kt");
        }

        @TestFactory
        @TestMetadata("standalone_multifile.kt")
        public Object testStandalone_multifile() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_multifile.kt");
        }

        @TestFactory
        @TestMetadata("standalone_notr_long_running.kt")
        public Object testStandalone_notr_long_running() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_notr_long_running.kt");
        }

        @TestFactory
        @TestMetadata("standalone_notr_long_running_and_verbose.kt")
        public Object testStandalone_notr_long_running_and_verbose() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_notr_long_running_and_verbose.kt");
        }

        @TestFactory
        @TestMetadata("standalone_notr_multifile_entry_point.kt")
        public Object testStandalone_notr_multifile_entry_point() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_notr_multifile_entry_point.kt");
        }

        @TestFactory
        @TestMetadata("standalone_notr_simple.kt")
        public Object testStandalone_notr_simple() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_notr_simple.kt");
        }

        @TestFactory
        @TestMetadata("standalone_notr_simple2.kt")
        public Object testStandalone_notr_simple2() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_notr_simple2.kt");
        }

        @TestFactory
        @TestMetadata("standalone_notr_simple_entry_point.kt")
        public Object testStandalone_notr_simple_entry_point() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_notr_simple_entry_point.kt");
        }

        @TestFactory
        @TestMetadata("standalone_notr_simple_entry_point2.kt")
        public Object testStandalone_notr_simple_entry_point2() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_notr_simple_entry_point2.kt");
        }

        @TestFactory
        @TestMetadata("standalone_notr_simple_with_input_and_output.kt")
        public Object testStandalone_notr_simple_with_input_and_output() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_notr_simple_with_input_and_output.kt");
        }

        @TestFactory
        @TestMetadata("standalone_notr_simple_with_output.kt")
        public Object testStandalone_notr_simple_with_output() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_notr_simple_with_output.kt");
        }

        @TestFactory
        @TestMetadata("standalone_notr_too_verbose.kt")
        public Object testStandalone_notr_too_verbose() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_notr_too_verbose.kt");
        }

        @TestFactory
        @TestMetadata("standalone_simple.kt")
        public Object testStandalone_simple() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_simple.kt");
        }

        @TestFactory
        @TestMetadata("standalone_simple_default_tr.kt")
        public Object testStandalone_simple_default_tr() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_simple_default_tr.kt");
        }

        @TestFactory
        @TestMetadata("standalone_simple_noexit_tr.kt")
        public Object testStandalone_simple_noexit_tr() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_simple_noexit_tr.kt");
        }

        @TestFactory
        @TestMetadata("standalone_simple_with_output.kt")
        public Object testStandalone_simple_with_output() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_simple_with_output.kt");
        }

        @TestFactory
        @TestMetadata("standalone_simple_worker_tr.kt")
        public Object testStandalone_simple_worker_tr() throws Exception {
            return dynamicTest("native/native.tests/testData/samples/standalone_simple_worker_tr.kt");
        }

        @Nested
        @TestMetadata("native/native.tests/testData/samples/inner")
        @TestDataPath("$PROJECT_ROOT")
        @Tag("infrastructure")
        @UseStandardTestCaseGroupProvider()
        public class Inner {
            @Test
            public void testAllFilesPresentInInner() throws Exception {
                KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("native/native.tests/testData/samples/inner"), Pattern.compile("^(.+)\\.kt$"), null, true);
            }

            @TestFactory
            @TestMetadata("regular_simple.kt")
            public Object testRegular_simple() throws Exception {
                return dynamicTest("native/native.tests/testData/samples/inner/regular_simple.kt");
            }
        }
    }

    @Nested
    @TestMetadata("native/native.tests/testData/samples2")
    @TestDataPath("$PROJECT_ROOT")
    @Tag("infrastructure")
    @UseStandardTestCaseGroupProvider()
    public class Samples2 {
        @Test
        public void testAllFilesPresentInSamples2() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("native/native.tests/testData/samples2"), Pattern.compile("^(.+)\\.kt$"), null, true);
        }

        @TestFactory
        @TestMetadata("regular_simple.kt")
        public Object testRegular_simple() throws Exception {
            return dynamicTest("native/native.tests/testData/samples2/regular_simple.kt");
        }
    }
}

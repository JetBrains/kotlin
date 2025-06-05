The Apache 2 license (given in full in [LICENSE.txt](LICENSE.txt)) applies to all code in this repository which is copyright
by JetBrains. The following sections of the repository contain third-party code, to which different licenses
may apply:

## Kotlin Compiler

The following modules contain third-party code and are incorporated into the Kotlin compiler and/or
the Kotlin IntelliJ IDEA plugin:

 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/inline/MaxStackFrameSizeAndLocalsCalculator.kt
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom
   
 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/inline/MaxLocalsCalculator.java
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/common/FastMethodAnalyzer.kt
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/common/InstructionLivenessAnalyzer.kt
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom 

 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/common/ControlFlowGraph.kt
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/fixStack/FastStackAnalyzer.kt
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/temporaryVals/FastStoreLoadAnalyzer.kt
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: eval4j/src/org/jetbrains/eval4j/interpreterLoop.kt
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/common/OptimizationBasicInterpreter.java
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: js/js.ast
     - License: BSD ([license/third_party/dart_LICENSE.txt][dart])
     - Origin: Originally part of the Dart compiler, (c) 2011 the Dart Project Authors,

 - Path: js/js.parser/src/com/google
      - License: Netscape Public License 1.1 ([license/third_party/rhino_LICENSE.txt][rhino])
      - Origin: Originally part of GWT, (C) 2007-08 Google Inc., distributed under the Apache 2 license. The code
        is derived from Rhino, (C) 1997-1999 Netscape Communications Corporation, distributed under the
        Netscape Public License.

 - Path: libraries/stdlib/src/kotlin/collections
      - License: Apache 2 ([license/third_party/gwt_license.txt][gwt])
      - Origin: Derived from GWT, (C) 2007-08 Google Inc.

 - Path: libraries/stdlib/src/kotlin/time
      - License: 3-clause BSD license ([license/third_party/threetenbp_license.txt][threetenbp])
      - Origin: Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos

 - Path: libraries/stdlib/js/src/kotlin/UnsignedJs.kt
      - License: Apache 2 ([license/third_party/guava_license.txt][guava])
      - Origin: Derived from Guava's UnsignedLongs, (C) 2011 The Guava Authors

 - Path: libraries/stdlib/jvm/src/kotlin/util/UnsignedJVM.kt
      - License: Apache 2 ([license/third_party/guava_license.txt][guava])
      - Origin: Derived from Guava's UnsignedLongs, (C) 2011 The Guava Authors

 - Path: kotlin-native/runtime/src/main/kotlin/kotlin/Unsigned.kt
      - License: Apache 2 ([license/third_party/guava_license.txt][guava])
      - Origin: Derived from Guava's UnsignedLongs, (C) 2011 The Guava Authors

 - Path: libraries/stdlib/jvm/src/kotlin/util/MathJVM.kt
      - License: Boost Software License 1.0 ([license/third_party/boost_LICENSE.txt][boost])
      - Origin: Derived from boost special math functions, Copyright Eric Ford & Hubert Holin 2001.

 - Path: libraries/stdlib/js/src/kotlin/collections
      - License: Apache 2 ([license/third_party/gwt_license.txt][gwt])
      - Origin: Derived from GWT, (C) 2007-08 Google Inc.

 - Path: libraries/stdlib/native-wasm/src/kotlin/collections
      - License: Apache 2 ([license/third_party/gwt_license.txt][gwt])
      - Origin: Derived from GWT, (C) 2007-08 Google Inc.

 - Path: libraries/stdlib/js/runtime/boxedLong.kt
      - License: Apache 2 ([license/third_party/closure-compiler_LICENSE.txt][closure-compiler])
      - Origin: Google Closure Library, Copyright 2009 The Closure Library Authors

 - Path: libraries/stdlib/js/src/kotlin/js/math.polyfills.kt
      - License: Boost Software License 1.0 ([license/third_party/boost_LICENSE.txt][boost])
      - Origin: Derived from boost special math functions, Copyright Eric Ford & Hubert Holin 2001.

 - Path: libraries/stdlib/wasm/internal/kotlin/wasm/internal/Number2String.kt
      - License: Apache 2 ([license/third_party/assemblyscript_license.txt][assemblyscript])
      - Origin: Derived from assemblyscript standard library

 - Path: libraries/tools/kotlin-power-assert
      - License: Apache 2 ([license/third_party/power_assert_license.txt][power-assert])
      - Origin: Copyright (C) 2020-2023 Brian Norman

 - Path: plugins/compose
      - License: Apache 2 ([license/third_party/compose_license.txt][compose])
      - Origin: Copyright 2019-2024 The Android Open Source Project

 - Path: plugins/lint/android-annotations
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: plugins/lint/lint-api
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: plugins/lint/lint-checks
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: plugins/lint/lint-idea
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: plugins/power-assert
      - License: Apache 2 ([license/third_party/power_assert_license.txt][power-assert])
      - Origin: Copyright (C) 2020-2023 Brian Norman

 - Path: wasm/ir/src/org/jetbrains/kotlin/wasm/ir/convertors
      - License: MIT ([license/third_party/asmble_license.txt][asmble])
      - Origin: Copyright (C) 2018 Chad Retz

 - Path: compiler/tests-common/tests/org/jetbrains/kotlin/codegen/ir/ComposeLikeGenerationExtension.kt
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Derived from JetPack Compose compiler plugin code, Copyright 2019 The Android Open Source Project

 - Path: libraries/stdlib/wasm/src/kotlin/text/FloatingPointConverter.kt
   - License: MIT ([license/third_party/asmble_license.txt][asmble])
   - Origin: Copyright (C) 2018 Chad Retz

 - Path: libraries/stdlib/wasm/src/kotlin/math/fdlibm/
   - License: SUN ([license/third_party/sun_license.txt][sun])
   - Origin: Copyright (C) 1993 by Sun Microsystems, Inc.

 - Path: kotlin-native/runtime/src/main/cpp/Utils.cpp
   - License: Boost Software License 1.0 ([license/third_party/boost_LICENSE.txt][boost])
   - Origin: Derived from boost hash functions, Copyright 2005-2014 Daniel James

 - Path: prepare/compiler/
    - License: Apache 2 ([license/third_party/opentelemetry_license.txt][opentelemetry])
    - Origin: Copyright The OpenTelemetry Authors

## Kotlin Test Data

The following source code is used for testing the Kotlin compiler and/or plugin and is not incorporated into
any distributions of the compiler, libraries or plugin:

 - Path: third-party/annotations/android
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: third-party/annotations/com/android
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: third-party/annotations/org/eclipse
      - License: Eclipse Public License v1.0 ([license/third_party/testdata/eclipse_license.txt][eclipse])
      - Origin: Eclipse JDT, Copyright (c) 2011, 2013 Stephan Herrmann and others.

 - Path: third-party/annotations/androidx
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: third-party/annotations/edu/umd/cs/findbugs
      - License: LGPL 2.1 ([license/third_party/testdata/findbugs_license.txt][findbugs])
      - Origin: Bytecode Analysis Framework, Copyright (C) 2005 University of Maryland

 - Path: third-party/java8-annotations/org/eclipse
      - License: Eclipse Public License v1.0 ([license/third_party/testdata/eclipse_license.txt][eclipse])
      - Origin: Eclipse JDT, Copyright (c) 2011, 2013 Stephan Herrmann and others.

 - Path: third-party/annotations/io/reactivex
      - License: Apache 2 ([license/third_party/testdata/rxjava_license.txt][rxjava])
      - Origin: RxJava, Copyright (c) 2016-present, RxJava Contributors

 - Path: third-party/java8-annotations/org/jspecify
      - License: Apache 2 ([license/third_party/testdata/jspecify_license.txt][jspecify])
      - Origin: JSpecify, Copyright (C) 2020 The JSpecify Authors

 - Path: third-party/java9-annotations/org/jspecify
      - License: Apache 2 ([license/third_party/testdata/jspecify_license.txt][jspecify])
      - Origin: JSpecify, Copyright (C) 2020 The JSpecify Authors

 - Path: third-party/annotations/lombok
      - License: MIT ([license/third_party/testdata/lombok_license.txt][lombok])
      - Origin: Project Lombok, Copyright (C) 2009-2013 The Project Lombok Authors

 - Path: idea/idea-android/tests/org/jetbrains/kotlin/android/AndroidTestBase.java
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: idea/testData/android/lintQuickfix/requiresApi/RequiresApi.java
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: idea/testData/android/lint/IntRange.java
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: idea/testData/android/lint/RequiresPermission.java
      - License: Apache 2 ([license/third_party/aosp_license.txt][aosp])
      - Origin: Copyright (C) 2011-15 The Android Open Source Project

 - Path: libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/allOpenSpring/src/org/springframework/stereotype/Component.java
      - License: Apache 2 ([license/third_party/testdata/spring_license.txt][spring])
      - Origin: Spring Framework, Copyright 2002-2007 the original author or authors.

 - Path: libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/AndroidDaggerProject
      - License: Apache 2 ([license/third_party/testdata/dagger_license.txt][dagger])
      - Origin: Dagger, Copyright (C) 2013 Square, Inc.

 - Path: libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/kapt2
      - License: Apache 2 ([license/third_party/testdata/dagger_license.txt][dagger])
      - Origin: Dagger, Copyright (C) 2013 Square, Inc.

 - Path: libraries/tools/kotlin-maven-plugin-test/src/it/test-allopen-spring/src/main/java/org/springframework/stereotype/Component.java
      - License: Apache 2 ([license/third_party/testdata/spring_license.txt][spring])
      - Origin: Spring Framework, Copyright 2002-2007 the original author or authors.

## Kotlin Tools and Libraries Tests

The following source code is used for testing the Kotlin tools and/or libraries and is not incorporated into
any distributions of the tools or libraries:

 - Path: libraries/stdlib/test/time
      - License: 3-clause BSD license ([license/third_party/threetenbp_license.txt][threetenbp])
      - Origin: Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos

 - Path: libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/PluginsDslIT.kt
      - License: Apache 2 ([license/third_party/gradle_license.txt][gradle])
      - Origin: Gradle, Copyright 2002-2017 Gradle, Inc.

 - Path: libraries/tools/kotlin-gradle-plugin-integration-tests/target/test-classes/testProject/noArgJpa/src/javax/persistence/Entity.java
      - License: Eclipse Public License v1.0 ([license/third_party/testdata/eclipse_license.txt][eclipse])
             and Eclipse Distribution License - v1.0 ([license/third_party/testdata/eclipse_distribution_license.txt][eclipse-distribution])
      - Origin: javax.persistence, Copyright (c) 2008, 2017 Sun Microsystems, Oracle Corporation.

 - Path: libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/noArgJpa/src/javax/persistence/Entity.java
      - License: Eclipse Public License v1.0 ([license/third_party/testdata/eclipse_license.txt][eclipse])
             and Eclipse Distribution License - v1.0 ([license/third_party/testdata/eclipse_distribution_license.txt][eclipse-distribution])
      - Origin: javax.persistence, Copyright (c) 2008, 2017 Sun Microsystems, Oracle Corporation.

 - Path: libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/noArgJpa/src/javax/persistence/Embeddable.java
      - License: Eclipse Public License v1.0 ([license/third_party/testdata/eclipse_license.txt][eclipse])
             and Eclipse Distribution License - v1.0 ([license/third_party/testdata/eclipse_distribution_license.txt][eclipse-distribution])
      - Origin: javax.persistence, Copyright (c) 2008, 2017 Sun Microsystems, Oracle Corporation.

 - Path: libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/powerAssertSimple
      - License: Apache 2 ([license/third_party/power_assert_license.txt][power-assert])
      - Origin: Copyright (C) 2020-2023 Brian Norman

 - Path: libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/nodejs/Platform.kt
      - License: Apache License 2.0 ([license/third_party/gradle-node-plugin_LICENSE.txt](third_party/gradle-node-plugin_LICENSE.txt))
      - Origin: Copyright (c) 2013 node-gradle/gradle-node-plugin

 - Path: libraries/tools/gradle/fus-statistics-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/fus/internal/ciUtil.kt
      - License: Apache 2 ([license/third_party/gradle_custom_user_plugin_license.txt](third_party/gradle_custom_user_plugin_license.txt))
      - Origin: Gradle, Copyright 2002-2025 Gradle, Inc.

## Example Code

The following code is provided as examples and is not incorporated into
any distributions of the compiler, libraries or plugin:

 - Path: libraries/examples/browser-example/src/js/jquery.js
      - License: MIT ([license/third_party/jquery_license.txt][jquery])
      - Origin: jQuery JavaScript Library v1.6.2, Copyright 2011, John Resig

 - Path: libraries/examples/browser-example-with-library/src/js/jquery.js
      - License: MIT ([license/third_party/jquery_license.txt][jquery])
      - Origin: jQuery JavaScript Library v1.6.2, Copyright 2011, John Resig

[aosp]: third_party/aosp_license.txt
[asm]: third_party/asm_license.txt
[asmble]: third_party/asmble_license.txt
[assemblyscript]: third_party/assemblyscript_license.txt
[boost]: third_party/boost_LICENSE.txt
[closure-compiler]: third_party/closure-compiler_LICENSE.txt
[compose]: third_party/compose_license.txt
[dagger]: third_party/testdata/dagger_license.txt
[dart]: third_party/dart_LICENSE.txt
[eclipse]: third_party/testdata/eclipse_license.txt
[eclipse-distribution]: third_party/testdata/eclipse_distribution_license.txt
[findbugs]: third_party/testdata/findbugs_license.txt
[gradle]: third_party/gradle_license.txt
[guava]: third_party/guava_license.txt
[gwt]: third_party/gwt_license.txt
[jquery]: third_party/jquery_license.txt
[jspecify]: third_party/testdata/jspecify_license.txt
[lombok]: third_party/testdata/lombok_license.txt
[power-assert]: third_party/power_assert_license.txt
[rhino]: third_party/rhino_LICENSE.txt
[rxjava]: third_party/testdata/rxjava_license.txt
[spring]: third_party/testdata/spring_license.txt
[sun]: third_party/sun_license.txt
[threetenbp]: third_party/threetenbp_license.txt
[opentelemetry]: third_party/opentelemetry_license.txt

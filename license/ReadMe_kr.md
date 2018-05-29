이 저장소에서 JetBrains에게 저작권이 있는 모든 코드에는 아파치2 라이센스(LICENSE.txt에 전체 내용이 있음)가 적용됩니다.
저장소의 다음 섹션에는 다른 라이센스가 적용될 수있는 제 3자 코드가 들어 있습니다.

## Kotlin Compiler

다음 모듈은 타사 코드를 포함하며 Kotlin 컴파일러 및/또는 Kotlin IntelliJ IDEA 플러그인에 통합되어 있습니다:

 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/inline/MaxStackFrameSizeAndLocalsCalculator.java
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/inline/MaxLocalsCalculator.java
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/common/MethodAnalyzer.kt
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: core/reflection.jvm/src/kotlin.reflect/jvm/internal/pcollections
     - License: MIT ([license/third_party/pcollections_LICENSE.txt][pcollections])
     - Origin: Derived from PCollections, A Persistent Java Collections Library (https://pcollections.org/)

 - Path: eval4j/src/org/jetbrains/eval4j/interpreterLoop.kt
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/common/OptimizationBasicInterpreter.java
     - License: BSD ([license/third_party/asm_license.txt][asm])
     - Origin: Derived from ASM: a very small and fast Java bytecode manipulation framework, Copyright (c) 2000-2011 INRIA, France Telecom

 - Path: js/js.ast
     - License: BSD ([license/third_party/dart_LICENSE.txt][dart])
     - Origin: Originally part of the Dart compiler, (c) 2011 the Dart Project Authors,

 - Path: js/js.inliner/src/org/jetbrains/kotlin/js/inline/FunctionInlineMutator.kt
     - License: BSD ([license/third_party/dart_LICENSE.txt][dart])
     - Origin: Originally part of the Dart compiler, (c) 2011 the Dart Project Authors,

 - Path: js/js.parser/src/com/google
      - License: Netscape Public License 1.1 ([license/third_party/rhino_LICENSE.txt][rhino])
      - Origin: Originally part of GWT, (C) 2007-08 Google Inc., distributed under the Apache 2 license. The code
        is derived from Rhino, (C) 1997-1999 Netscape Communications Corporation, distributed under the
        Netscape Public License.

 - Path: js/js.translator/qunit/qunit.js
      - License: MIT ([license/third_party/qunit_license.txt][qunit])
      - Origin: QUnit, Copyright (c) 2012 John Resig, Jörn Zaefferer,

 - Path: libraries/stdlib/src/kotlin/collections
      - License: Apache 2 ([license/third_party/gwt_license.txt][gwt])
      - Origin: Derived from GWT, (C) 2007-08 Google Inc.

 - Path: libraries/stdlib/jvm/src/kotlin/util/MathJVM.kt
      - License: Boost Software License 1.0 ([license/third_party/boost_LICENSE.txt][boost])
      - Origin: Derived from boost special math functions, Copyright Eric Ford & Hubert Holin 2001.

 - Path: libraries/stdlib/js/src/kotlin/collections
      - License: Apache 2 ([license/third_party/gwt_license.txt][gwt])
      - Origin: Derived from GWT, (C) 2007-08 Google Inc.

 - Path: libraries/stdlib/js/src/js/long.js
      - License: Apache 2 ([license/third_party/closure-compiler_LICENSE.txt][closure-compiler])
      - Origin: Google Closure Library, Copyright 2009 The Closure Library Authors

 - Path: libraries/stdlib/js/src/js/polyfills.js
      - License: Boost Software License 1.0 ([license/third_party/boost_LICENSE.txt][boost])
      - Origin: Derived from boost special math functions, Copyright Eric Ford & Hubert Holin 2001.

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

## Kotlin 테스트 

다음 소스 코드는 Kotlin 컴파일러 및/또는 플러그인을 테스트하는데 사용되며 컴파일러, 라이브러리 또는 플러그인의 배포판에 통합되지 않습니다:

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

 - Path: third-party/jdk8-annotations/org/eclipse
      - License: Eclipse Public License v1.0 ([license/third_party/testdata/eclipse_license.txt][eclipse])
      - Origin: Eclipse JDT, Copyright (c) 2011, 2013 Stephan Herrmann and others.

 - Path: third-party/annotations/io/reactivex
      - License: Apache 2 ([license/third_party/testdata/rxjava_license.txt][rxjava])
      - Origin: RxJava, Copyright (c) 2016-present, RxJava Contributors

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

## Kotlin 도구와 라이브러리 테스트

다음 소스 코드는 Kotlin 도구 및/또는 라이브러리를 테스트하는데 사용되며 도구 또는 라이브러리의 배포판에 통합되지 않습니다:

 - Path: libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/PluginsDslIT.kt
      - License: Apache 2 ([license/third_party/testdata/gradle_license.txt][gradle])
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

## 예제 코드

다음 코드는 예제로 제공되며 컴파일러, 라이브러리 또는 플러그인 배포에 통합되지 않습니다:

 - Path: libraries/examples/browser-example/src/js/jquery.js
      - License: MIT ([license/third_party/jquery_license.txt][jquery])
      - Origin: jQuery JavaScript Library v1.6.2, Copyright 2011, John Resig

 - Path: libraries/examples/browser-example-with-library/src/js/jquery.js
      - License: MIT ([license/third_party/jquery_license.txt][jquery])
      - Origin: jQuery JavaScript Library v1.6.2, Copyright 2011, John Resig

[aosp]: third_party/aosp_license.txt
[asm]: third_party/asm_license.txt
[boost]: third_party/boost_LICENSE.txt
[closure-compiler]: third_party/closure-compiler_LICENSE.txt
[dagger]: third_party/testdata/dagger_license.txt
[dart]: third_party/dart_LICENSE.txt
[eclipse]: third_party/testdata/eclipse_license.txt
[eclipse-distribution]: third_party/testdata/eclipse_distribution_license.txt
[findbugs]: third_party/testdata/findbugs_license.txt
[gradle]: third_party/testdata/gradle_license.txt
[gwt]: third_party/gwt_license.txt
[jquery]: third_party/jquery_license.txt
[lombok]: third_party/testdata/lombok_license.txt
[pcollections]: third_party/pcollections_LICENSE.txt
[qunit]: third_party/qunit_license.txt
[rhino]: third_party/rhino_LICENSE.txt
[rxjava]: third_party/testdata/rxjava_license.txt
[spring]: third_party/testdata/spring_license.txt

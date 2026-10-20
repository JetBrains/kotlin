/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

import org.intellij.lang.annotations.Language

// Taken from Xcode 27.2 wizard
@Language("JSON5")
internal val sampleXcprojProject = """
    {
      "default-configuration": "Release",
      "configurations": [
        "Debug",
        "Release",
      ],
      "localizations": {
        "development": "en",
        "supported": [
          "Base",
        ],
      },
      "files": [
        { "kind": "folder", "path": "MyApp", "target-membership": [ "MyApp" ] },
        {
          "kind": "group",
          "name": "Products",
          "children": [
            { "path": "<PRODUCTS>/MyApp.app", "id": "000000000000000000000120", "type": "wrapper.application", "index": false },
          ],
        },
      ],
      "targets": [
        {
          "name": "MyApp",
          "id": "000000000000000100000000",
          "product": "Products/MyApp.app",
          "product-type": "application",
          "build-phases": [
            "compile-sources",
            "frameworks",
            "resources",
            {"kind": "frameworks", "id": "222222222222222222222222", "future-phase-field": [1, null]},
            {"kind": "script", "name": "Other script", "shell": "/bin/sh", "script": ["echo hello", "echo world"], "future-script-field": true},
          ],
          "dependencies": [
            "ExistingDependencyLiteral",
            {
              "kind": "opaque",
              "foo": "bar",
            },
          ],
          "package-product-members": [
            {
                "product-name": "Something",
                "build-phase": {
                  "build-phase": "frameworks"
                }
            },
          ],
          "build-settings": {
            "CODE_SIGN_STYLE": "Automatic",
            "CURRENT_PROJECT_VERSION": "1",
            "ENABLE_APP_SANDBOX": "YES",
            "ENABLE_PREVIEWS": "YES",
            "ENABLE_USER_SELECTED_FILES": "readonly",
            "GENERATE_INFOPLIST_FILE": "YES",
            "INFOPLIST_KEY_UIApplicationSceneManifest_Generation[sdk=iphoneos*]": "YES",
            "INFOPLIST_KEY_UIApplicationSceneManifest_Generation[sdk=iphonesimulator*]": "YES",
            "INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents[sdk=iphoneos*]": "YES",
            "INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents[sdk=iphonesimulator*]": "YES",
            "INFOPLIST_KEY_UILaunchScreen_Generation[sdk=iphoneos*]": "YES",
            "INFOPLIST_KEY_UILaunchScreen_Generation[sdk=iphonesimulator*]": "YES",
            "INFOPLIST_KEY_UIStatusBarStyle[sdk=iphoneos*]": "UIStatusBarStyleDefault",
            "INFOPLIST_KEY_UIStatusBarStyle[sdk=iphonesimulator*]": "UIStatusBarStyleDefault",
            "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad": "UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
            "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone": "UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
            "LD_RUNPATH_SEARCH_PATHS": "@executable_path/Frameworks",
            "LD_RUNPATH_SEARCH_PATHS[sdk=macosx*]": "@executable_path/../Frameworks",
            "MARKETING_VERSION": "1.0",
            "PRODUCT_BUNDLE_IDENTIFIER": "devplaceholder.$(PROJECT_UNIQUE_VALUE:identifier).$(PRODUCT_NAME:rfc1034identifier)",
            "PRODUCT_NAME": "$(TARGET_NAME)",
            "REGISTER_APP_GROUPS": "YES",
            "SDKROOT": "auto",
            "STRING_CATALOG_GENERATE_SYMBOLS": "YES",
            "SUPPORTED_PLATFORMS": "iphoneos iphonesimulator xros xrsimulator",
            "SWIFT_APPROACHABLE_CONCURRENCY": "YES",
            "SWIFT_DEFAULT_ACTOR_ISOLATION": "MainActor",
            "SWIFT_EMIT_LOC_STRINGS": "YES",
            "SWIFT_UPCOMING_FEATURE_MEMBER_IMPORT_VISIBILITY": "YES",
            "SWIFT_VERSION": "5.0",
            "TARGETED_DEVICE_FAMILY": "1,2,7",
          },
          "future-target-field": {"values": [null, {"nested": "preserved"}]},
        },
        {
          "kind": "aggregate",
          "name": "Prepare",
          "id": "333333333333333333333333",
          "future-target-field": {"untouched": true}
        }
      ],
      "build-settings": {
        "ALWAYS_SEARCH_USER_PATHS": "NO",
        "ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS": "YES",
        "CLANG_ANALYZER_NONNULL": "YES",
        "CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION": "YES_AGGRESSIVE",
        "CLANG_CXX_LANGUAGE_STANDARD": "gnu++20",
        "CLANG_ENABLE_MODULES": "YES",
        "CLANG_ENABLE_OBJC_ARC": "YES",
        "CLANG_ENABLE_OBJC_WEAK": "YES",
        "CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING": "YES",
        "CLANG_WARN_BOOL_CONVERSION": "YES",
        "CLANG_WARN_COMMA": "YES",
        "CLANG_WARN_CONSTANT_CONVERSION": "YES",
        "CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS": "YES",
        "CLANG_WARN_DIRECT_OBJC_ISA_USAGE": "YES_ERROR",
        "CLANG_WARN_DOCUMENTATION_COMMENTS": "YES",
        "CLANG_WARN_EMPTY_BODY": "YES",
        "CLANG_WARN_ENUM_CONVERSION": "YES",
        "CLANG_WARN_INFINITE_RECURSION": "YES",
        "CLANG_WARN_INT_CONVERSION": "YES",
        "CLANG_WARN_NON_LITERAL_NULL_CONVERSION": "YES",
        "CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF": "YES",
        "CLANG_WARN_OBJC_LITERAL_CONVERSION": "YES",
        "CLANG_WARN_OBJC_ROOT_CLASS": "YES_ERROR",
        "CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER": "YES",
        "CLANG_WARN_RANGE_LOOP_ANALYSIS": "YES",
        "CLANG_WARN_STRICT_PROTOTYPES": "YES",
        "CLANG_WARN_SUSPICIOUS_MOVE": "YES",
        "CLANG_WARN_UNGUARDED_AVAILABILITY": "YES_AGGRESSIVE",
        "CLANG_WARN_UNREACHABLE_CODE": "YES",
        "CLANG_WARN__DUPLICATE_METHOD_MATCH": "YES",
        "COPY_PHASE_STRIP": "NO",
        "DEAD_CODE_STRIPPING": "YES",
        "DEBUG_INFORMATION_FORMAT[config=Debug]": "dwarf",
        "DEBUG_INFORMATION_FORMAT[config=Release]": "dwarf-with-dsym",
        "DRIVERKIT_DEPLOYMENT_TARGET": "27.2",
        "ENABLE_NS_ASSERTIONS[config=Release]": "NO",
        "ENABLE_STRICT_OBJC_MSGSEND": "YES",
        "ENABLE_TESTABILITY[config=Debug]": "YES",
        "ENABLE_USER_SCRIPT_SANDBOXING": "YES",
        "GCC_C_LANGUAGE_STANDARD": "gnu17",
        "GCC_DYNAMIC_NO_PIC[config=Debug]": "NO",
        "GCC_NO_COMMON_BLOCKS": "YES",
        "GCC_OPTIMIZATION_LEVEL[config=Debug]": "0",
        "GCC_PREPROCESSOR_DEFINITIONS[config=Debug]": [
          "DEBUG=1",
          "$(inherited)",
        ],
        "GCC_WARN_64_TO_32_BIT_CONVERSION": "YES",
        "GCC_WARN_ABOUT_RETURN_TYPE": "YES_ERROR",
        "GCC_WARN_UNDECLARED_SELECTOR": "YES",
        "GCC_WARN_UNINITIALIZED_AUTOS": "YES_AGGRESSIVE",
        "GCC_WARN_UNUSED_FUNCTION": "YES",
        "GCC_WARN_UNUSED_VARIABLE": "YES",
        "IPHONEOS_DEPLOYMENT_TARGET": "27.2",
        "LOCALIZATION_PREFERS_STRING_CATALOGS": "YES",
        "MACOSX_DEPLOYMENT_TARGET": "26.7.1",
        "MTL_ENABLE_DEBUG_INFO[config=Debug]": "INCLUDE_SOURCE",
        "MTL_ENABLE_DEBUG_INFO[config=Release]": "NO",
        "MTL_FAST_MATH": "YES",
        "ONLY_ACTIVE_ARCH[config=Debug]": "YES",
        "PROJECT_UNIQUE_VALUE": "WAIQE639",
        "STRING_CATALOG_GENERATE_SYMBOLS": "YES",
        "SWIFT_ACTIVE_COMPILATION_CONDITIONS[config=Debug]": "DEBUG $(inherited)",
        "SWIFT_COMPILATION_MODE[config=Release]": "wholemodule",
        "SWIFT_OPTIMIZATION_LEVEL[config=Debug]": "-Onone",
        "TVOS_DEPLOYMENT_TARGET": "27.2",
        "WATCHOS_DEPLOYMENT_TARGET": "27.2",
        "XROS_DEPLOYMENT_TARGET": "27.2",
      },
      "last-upgrade": "27.0",
      "last-swift-update": "26.3",
      "future-project-field": {"values": [null, 1, false, {"nested": "preserved"}]},
    }
""".trimIndent()

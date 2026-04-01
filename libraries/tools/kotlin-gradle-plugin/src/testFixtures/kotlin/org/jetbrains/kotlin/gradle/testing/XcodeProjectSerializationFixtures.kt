/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

object XcodeProjectSerializationFixtures {
    /**
     * Created from "/usr/bin/plutil -convert json /path/to/project.pbxproj" from a wizard Xcode project
     */
    val sampleXcodeProjectWithEmbedAndSignIntegration = """
        {
          "classes": {},
          "objectVersion": "77",
          "archiveVersion": "1",
          "objects": {
            "7ED515C418B8543FC36B6C75": {
              "isa": "PBXSourcesBuildPhase",
              "buildActionMask": "2147483647",
              "files": [],
              "runOnlyForDeploymentPostprocessing": "0"
            },
            "4C145A11DE14CF58089D0E0D": {
              "isa": "XCConfigurationList",
              "defaultConfigurationIsVisible": "0",
              "defaultConfigurationName": "Release",
              "buildConfigurations": [
                "E3A5F60AFEDBAAA1B57BC5DF",
                "0CE831B4CD0EE81E604BECD1"
              ]
            },
            "F8402C682F4F3A30009C6D26": {
              "isa": "XCLocalSwiftPackageReference",
              "relativePath": "../sub/LocalPackage"
            },
            "F8402C692F4F3A30009C6D26": {
              "isa": "XCSwiftPackageProductDependency",
              "productName": "LocalPackage"
            },
            "5F0DB50324B04E82C0A2B00B": {
              "buildConfigurationList": "4C145A11DE14CF58089D0E0D",
              "packageReferences": [
                "060E5E5CC791EDC4E478279F",
                "F8402C682F4F3A30009C6D26"
              ],
              "targets": [
                "A1B0EED38E5AD245C3EB3CD9"
              ],
              "minimizedProjectReferenceProxies": "1",
              "developmentRegion": "en",
              "knownRegions": [
                "en",
                "Base"
              ],
              "isa": "PBXProject",
              "productRefGroup": "72D9CDEA21C9C62FCF4AB03F",
              "projectDirPath": "",
              "attributes": {
                "LastSwiftUpdateCheck": "1620",
                "BuildIndependentTargetsInParallel": "1",
                "LastUpgradeCheck": "1620",
                "TargetAttributes": {
                  "A1B0EED38E5AD245C3EB3CD9": {
                    "CreatedOnToolsVersion": "16.2"
                  }
                }
              },
              "hasScannedForEncodings": "0",
              "mainGroup": "1C4A84397000E65BBD1ED68C",
              "preferredProjectObjectVersion": "77",
              "projectRoot": ""
            },
            "72D9CDEA21C9C62FCF4AB03F": {
              "isa": "PBXGroup",
              "name": "Products",
              "children": [
                "074BE035A195D24551A37F7B"
              ],
              "sourceTree": "<group>"
            },
            "F8402C672F4F3799009C6D26": {
              "isa": "PBXBuildFile",
              "productRef": "1F767540D0F8DD22D9D40770",
              "settings": {
                "ATTRIBUTES": [
                  "CodeSignOnCopy"
                ]
              }
            },
            "77CCE44CD2AA61D4434C2A7A": {
              "isa": "PBXFileSystemSynchronizedRootGroup",
              "path": "Configuration",
              "sourceTree": "<group>"
            },
            "F8402C6A2F4F3A30009C6D26": {
              "isa": "PBXBuildFile",
              "productRef": "F8402C692F4F3A30009C6D26"
            },
            "6D0E1D11300057C232586690": {
              "isa": "XCBuildConfiguration",
              "buildSettings": {
                "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad": "UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
                "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone": "UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
                "ENABLE_DEBUG_DYLIB": "NO",
                "DEVELOPMENT_TEAM": "K2W3LLB22L",
                "PRODUCT_BUNDLE_IDENTIFIER": "org.example.dynamicframeworkwithsubproject2.dynamicframeworkwithsubproject3",
                "ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME": "AccentColor",
                "PROVISIONING_PROFILE_SPECIFIER": "",
                "DEVELOPMENT_ASSET_PATHS": "\"iosApp/Preview Content\"",
                "INFOPLIST_KEY_UILaunchScreen_Generation": "YES",
                "OTHER_LDFLAGS": "",
                "CODE_SIGN_STYLE": "Automatic",
                "TARGETED_DEVICE_FAMILY": "1,2",
                "ASSETCATALOG_COMPILER_APPICON_NAME": "AppIcon",
                "LD_RUNPATH_SEARCH_PATHS": [
                  "$(inherited)",
                  "@executable_path/Frameworks"
                ],
                "SWIFT_VERSION": "5.0",
                "IPHONEOS_DEPLOYMENT_TARGET": "17.0",
                "INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents": "YES",
                "CODE_SIGN_IDENTITY": "Apple Development",
                "ENABLE_PREVIEWS": "YES",
                "ARCHS": "arm64",
                "INFOPLIST_KEY_UIApplicationSceneManifest_Generation": "YES",
                "SWIFT_EMIT_LOC_STRINGS": "YES",
                "INFOPLIST_FILE": "iosApp/Info.plist",
                "GENERATE_INFOPLIST_FILE": "YES"
              },
              "name": "Release"
            },
            "074BE035A195D24551A37F7B": {
              "path": "dynamicframeworkwithsubproject2.app",
              "isa": "PBXFileReference",
              "includeInIndex": "0",
              "explicitFileType": "wrapper.application",
              "sourceTree": "BUILT_PRODUCTS_DIR"
            },
            "6CAE366E6154E6A2A4027414": {
              "isa": "PBXFrameworksBuildPhase",
              "buildActionMask": "2147483647",
              "files": [
                "5A3773A3D0367647F2CD676D",
                "F8402C6A2F4F3A30009C6D26"
              ],
              "runOnlyForDeploymentPostprocessing": "0"
            },
            "9FC2545CCEF0B94DCE5E949C": {
              "isa": "XCBuildConfiguration",
              "buildSettings": {
                "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad": "UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
                "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone": "UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
                "ENABLE_DEBUG_DYLIB": "NO",
                "DEVELOPMENT_TEAM": "K2W3LLB22L",
                "PRODUCT_BUNDLE_IDENTIFIER": "org.example.dynamicframeworkwithsubproject2.dynamicframeworkwithsubproject3",
                "ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME": "AccentColor",
                "PROVISIONING_PROFILE_SPECIFIER": "",
                "DEVELOPMENT_ASSET_PATHS": "\"iosApp/Preview Content\"",
                "INFOPLIST_KEY_UILaunchScreen_Generation": "YES",
                "OTHER_LDFLAGS": "",
                "CODE_SIGN_STYLE": "Automatic",
                "TARGETED_DEVICE_FAMILY": "1,2",
                "ASSETCATALOG_COMPILER_APPICON_NAME": "AppIcon",
                "LD_RUNPATH_SEARCH_PATHS": [
                  "$(inherited)",
                  "@executable_path/Frameworks"
                ],
                "SWIFT_VERSION": "5.0",
                "IPHONEOS_DEPLOYMENT_TARGET": "17.0",
                "INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents": "YES",
                "CODE_SIGN_IDENTITY": "Apple Development",
                "ENABLE_PREVIEWS": "YES",
                "ARCHS": "arm64",
                "INFOPLIST_KEY_UIApplicationSceneManifest_Generation": "YES",
                "SWIFT_EMIT_LOC_STRINGS": "YES",
                "INFOPLIST_FILE": "iosApp/Info.plist",
                "GENERATE_INFOPLIST_FILE": "YES"
              },
              "name": "Debug"
            },
            "E3A5F60AFEDBAAA1B57BC5DF": {
              "isa": "XCBuildConfiguration",
              "buildSettings": {
                "CLANG_WARN_RANGE_LOOP_ANALYSIS": "YES",
                "ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS": "YES",
                "SWIFT_OPTIMIZATION_LEVEL": "-Onone",
                "CLANG_WARN_INFINITE_RECURSION": "YES",
                "ONLY_ACTIVE_ARCH": "YES",
                "GCC_WARN_UNUSED_VARIABLE": "YES",
                "CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING": "YES",
                "CLANG_WARN_UNGUARDED_AVAILABILITY": "YES_AGGRESSIVE",
                "COPY_PHASE_STRIP": "NO",
                "CLANG_WARN_BOOL_CONVERSION": "YES",
                "CLANG_WARN_ENUM_CONVERSION": "YES",
                "CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS": "YES",
                "CLANG_WARN_COMMA": "YES",
                "SWIFT_ACTIVE_COMPILATION_CONDITIONS": "DEBUG $(inherited)",
                "CLANG_WARN_INT_CONVERSION": "YES",
                "ENABLE_STRICT_OBJC_MSGSEND": "YES",
                "CLANG_WARN_DOCUMENTATION_COMMENTS": "YES",
                "CLANG_WARN_SUSPICIOUS_MOVE": "YES",
                "SDKROOT": "iphoneos",
                "CLANG_WARN_EMPTY_BODY": "YES",
                "MTL_ENABLE_DEBUG_INFO": "INCLUDE_SOURCE",
                "MTL_FAST_MATH": "YES",
                "IPHONEOS_DEPLOYMENT_TARGET": "18.2",
                "CLANG_ENABLE_OBJC_ARC": "YES",
                "GCC_WARN_UNINITIALIZED_AUTOS": "YES_AGGRESSIVE",
                "GCC_C_LANGUAGE_STANDARD": "gnu17",
                "CLANG_ENABLE_MODULES": "YES",
                "ALWAYS_SEARCH_USER_PATHS": "NO",
                "ENABLE_TESTABILITY": "YES",
                "CLANG_WARN_DIRECT_OBJC_ISA_USAGE": "YES_ERROR",
                "CLANG_WARN_CONSTANT_CONVERSION": "YES",
                "GCC_NO_COMMON_BLOCKS": "YES",
                "CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF": "YES",
                "CLANG_WARN_OBJC_ROOT_CLASS": "YES_ERROR",
                "LOCALIZATION_PREFERS_STRING_CATALOGS": "YES",
                "CLANG_CXX_LANGUAGE_STANDARD": "gnu++20",
                "GCC_WARN_64_TO_32_BIT_CONVERSION": "YES",
                "CLANG_WARN__DUPLICATE_METHOD_MATCH": "YES",
                "GCC_DYNAMIC_NO_PIC": "NO",
                "DEBUG_INFORMATION_FORMAT": "dwarf",
                "GCC_WARN_UNDECLARED_SELECTOR": "YES",
                "CLANG_ANALYZER_NONNULL": "YES",
                "CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION": "YES_AGGRESSIVE",
                "CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER": "YES",
                "CLANG_ENABLE_OBJC_WEAK": "YES",
                "CLANG_WARN_STRICT_PROTOTYPES": "YES",
                "GCC_OPTIMIZATION_LEVEL": "0",
                "ENABLE_USER_SCRIPT_SANDBOXING": "NO",
                "GCC_WARN_ABOUT_RETURN_TYPE": "YES_ERROR",
                "GCC_WARN_UNUSED_FUNCTION": "YES",
                "CLANG_WARN_UNREACHABLE_CODE": "YES",
                "CLANG_WARN_NON_LITERAL_NULL_CONVERSION": "YES",
                "GCC_PREPROCESSOR_DEFINITIONS": [
                  "DEBUG=1",
                  "$(inherited)"
                ],
                "CLANG_WARN_OBJC_LITERAL_CONVERSION": "YES"
              },
              "baseConfigurationReferenceRelativePath": "Config.xcconfig",
              "name": "Debug",
              "baseConfigurationReferenceAnchor": "77CCE44CD2AA61D4434C2A7A"
            },
            "1C4B43741FF179B6CED334C1": {
              "isa": "PBXResourcesBuildPhase",
              "buildActionMask": "2147483647",
              "files": [],
              "runOnlyForDeploymentPostprocessing": "0"
            },
            "1C4A84397000E65BBD1ED68C": {
              "isa": "PBXGroup",
              "sourceTree": "<group>",
              "children": [
                "77CCE44CD2AA61D4434C2A7A",
                "DB71335302B6FFF4617F5524",
                "72D9CDEA21C9C62FCF4AB03F"
              ]
            },
            "A1B0EED38E5AD245C3EB3CD9": {
              "buildRules": [],
              "buildConfigurationList": "8504E73408E0DC9CC68C54AD",
              "productReference": "074BE035A195D24551A37F7B",
              "productType": "com.apple.product-type.application",
              "packageProductDependencies": [
                "1F767540D0F8DD22D9D40770",
                "F8402C692F4F3A30009C6D26"
              ],
              "productName": "iosApp",
              "isa": "PBXNativeTarget",
              "fileSystemSynchronizedGroups": [
                "DB71335302B6FFF4617F5524"
              ],
              "dependencies": [],
              "buildPhases": [
                "6A41626B4D2024E7D82B2A2C",
                "7ED515C418B8543FC36B6C75",
                "6CAE366E6154E6A2A4027414",
                "1C4B43741FF179B6CED334C1",
                "F8650A262ECF65E70033160A"
              ],
              "name": "iosApp"
            },
            "0CE831B4CD0EE81E604BECD1": {
              "isa": "XCBuildConfiguration",
              "buildSettings": {
                "CLANG_WARN_UNGUARDED_AVAILABILITY": "YES_AGGRESSIVE",
                "CLANG_WARN_SUSPICIOUS_MOVE": "YES",
                "CLANG_WARN_DIRECT_OBJC_ISA_USAGE": "YES_ERROR",
                "CLANG_ENABLE_OBJC_ARC": "YES",
                "CLANG_ENABLE_OBJC_WEAK": "YES",
                "CLANG_WARN__DUPLICATE_METHOD_MATCH": "YES",
                "IPHONEOS_DEPLOYMENT_TARGET": "18.2",
                "SWIFT_COMPILATION_MODE": "wholemodule",
                "CLANG_WARN_INFINITE_RECURSION": "YES",
                "CLANG_WARN_OBJC_LITERAL_CONVERSION": "YES",
                "DEBUG_INFORMATION_FORMAT": "dwarf-with-dsym",
                "CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF": "YES",
                "CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION": "YES_AGGRESSIVE",
                "SDKROOT": "iphoneos",
                "CLANG_CXX_LANGUAGE_STANDARD": "gnu++20",
                "LOCALIZATION_PREFERS_STRING_CATALOGS": "YES",
                "CLANG_WARN_BOOL_CONVERSION": "YES",
                "CLANG_WARN_UNREACHABLE_CODE": "YES",
                "ENABLE_STRICT_OBJC_MSGSEND": "YES",
                "CLANG_ANALYZER_NONNULL": "YES",
                "CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING": "YES",
                "CLANG_WARN_STRICT_PROTOTYPES": "YES",
                "CLANG_WARN_ENUM_CONVERSION": "YES",
                "CLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER": "YES",
                "GCC_WARN_64_TO_32_BIT_CONVERSION": "YES",
                "ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS": "YES",
                "CLANG_WARN_EMPTY_BODY": "YES",
                "GCC_WARN_ABOUT_RETURN_TYPE": "YES_ERROR",
                "GCC_WARN_UNINITIALIZED_AUTOS": "YES_AGGRESSIVE",
                "CLANG_WARN_COMMA": "YES",
                "GCC_C_LANGUAGE_STANDARD": "gnu17",
                "GCC_WARN_UNUSED_VARIABLE": "YES",
                "CLANG_WARN_RANGE_LOOP_ANALYSIS": "YES",
                "CLANG_ENABLE_MODULES": "YES",
                "CLANG_WARN_INT_CONVERSION": "YES",
                "ALWAYS_SEARCH_USER_PATHS": "NO",
                "CLANG_WARN_OBJC_ROOT_CLASS": "YES_ERROR",
                "CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS": "YES",
                "COPY_PHASE_STRIP": "NO",
                "CLANG_WARN_CONSTANT_CONVERSION": "YES",
                "ENABLE_USER_SCRIPT_SANDBOXING": "NO",
                "GCC_NO_COMMON_BLOCKS": "YES",
                "MTL_ENABLE_DEBUG_INFO": "NO",
                "CLANG_WARN_NON_LITERAL_NULL_CONVERSION": "YES",
                "MTL_FAST_MATH": "YES",
                "VALIDATE_PRODUCT": "YES",
                "GCC_WARN_UNUSED_FUNCTION": "YES",
                "ENABLE_NS_ASSERTIONS": "NO",
                "CLANG_WARN_DOCUMENTATION_COMMENTS": "YES",
                "GCC_WARN_UNDECLARED_SELECTOR": "YES"
              },
              "baseConfigurationReferenceRelativePath": "Config.xcconfig",
              "name": "Release",
              "baseConfigurationReferenceAnchor": "77CCE44CD2AA61D4434C2A7A"
            },
            "DB71335302B6FFF4617F5524": {
              "path": "iosApp",
              "isa": "PBXFileSystemSynchronizedRootGroup",
              "exceptions": [
                "883879F05CA097EB3E7BCEB2"
              ],
              "sourceTree": "<group>"
            },
            "5A3773A3D0367647F2CD676D": {
              "isa": "PBXBuildFile",
              "productRef": "1F767540D0F8DD22D9D40770"
            },
            "6A41626B4D2024E7D82B2A2C": {
              "buildActionMask": "2147483647",
              "runOnlyForDeploymentPostprocessing": "0",
              "outputPaths": [],
              "shellPath": "/bin/sh",
              "alwaysOutOfDate": "1",
              "inputFileListPaths": [],
              "isa": "PBXShellScriptBuildPhase",
              "shellScript": "if [ \"YES\" = \"${'$'}OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED\" ]; then\n  echo \"Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \\\"YES\\\"\"\n  exit 0\nfi\ncd \"${'$'}SRCROOT/..\"\n./gradlew :shared:embedAndSignAppleFrameworkForXcode -i\n",
              "files": [],
              "inputPaths": [],
              "outputFileListPaths": [],
              "name": "Compile Kotlin Framework"
            },
            "F8650A262ECF65E70033160A": {
              "buildActionMask": "2147483647",
              "dstSubfolderSpec": "10",
              "isa": "PBXCopyFilesBuildPhase",
              "runOnlyForDeploymentPostprocessing": "0",
              "dstPath": "",
              "files": [
                "F8402C672F4F3799009C6D26"
              ],
              "name": "Embed Frameworks"
            },
            "060E5E5CC791EDC4E478279F": {
              "isa": "XCLocalSwiftPackageReference",
              "relativePath": "KotlinMultiplatformLinkedPackage"
            },
            "1F767540D0F8DD22D9D40770": {
              "isa": "XCSwiftPackageProductDependency",
              "productName": "KotlinMultiplatformLinkedPackage"
            },
            "8504E73408E0DC9CC68C54AD": {
              "isa": "XCConfigurationList",
              "defaultConfigurationIsVisible": "0",
              "defaultConfigurationName": "Release",
              "buildConfigurations": [
                "9FC2545CCEF0B94DCE5E949C",
                "6D0E1D11300057C232586690"
              ]
            },
            "883879F05CA097EB3E7BCEB2": {
              "isa": "PBXFileSystemSynchronizedBuildFileExceptionSet",
              "membershipExceptions": [
                "Info.plist"
              ],
              "target": "A1B0EED38E5AD245C3EB3CD9"
            }
          },
          "rootObject": "5F0DB50324B04E82C0A2B00B"
        }
    """.trimIndent()
}
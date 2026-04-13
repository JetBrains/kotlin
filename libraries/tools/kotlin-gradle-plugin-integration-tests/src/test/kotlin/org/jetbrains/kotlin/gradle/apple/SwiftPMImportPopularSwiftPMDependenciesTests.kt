/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.api.file.ProjectLayout
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.*
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.*

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
@SwiftPMImportGradlePluginTests
class SwiftPMImportPopularSwiftPMDependenciesTests : KGPBaseTest() {

    @DisplayName("direct dependency on Firebase")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on Firebase`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "firestoreForApp" to """
        swiftPMImport.emptyxcode/FIRFirestoreMeta.firestoreForApp|objc:firestoreForApp:[1]
        swiftPMImport.emptyxcode/FIRFirestoreMeta.firestoreForApp|objc:firestoreForApp:database:[1]
    """.trimIndent(),
            "FIRAnalytics" to """
        swiftPMImport.emptyxcode/FIRAnalytics.<init>|objc:init#Constructor[1]
        swiftPMImport.emptyxcode/FIRAnalytics.Companion|null[1]
        swiftPMImport.emptyxcode/FIRAnalytics.init|objc:init[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.<init>|<init>(){}[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.allocWithZone|objc:allocWithZone:[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.alloc|objc:alloc[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.appInstanceID|objc:appInstanceID[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.logEventWithName|objc:logEventWithName:parameters:[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.new|objc:new[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.resetAnalyticsData|objc:resetAnalyticsData[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.sessionIDWithCompletion|objc:sessionIDWithCompletion:[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.setAnalyticsCollectionEnabled|objc:setAnalyticsCollectionEnabled:[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.setDefaultEventParameters|objc:setDefaultEventParameters:[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.setSessionTimeoutInterval|objc:setSessionTimeoutInterval:[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.setUserID|objc:setUserID:[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta.setUserPropertyString|objc:setUserPropertyString:forName:[1]
        swiftPMImport.emptyxcode/FIRAnalyticsMeta|null[1]
        swiftPMImport.emptyxcode/FIRAnalytics|null[1]
        swiftPMImport.emptyxcode/handleEventsForBackgroundURLSession|FIRAnalyticsMeta.objc:handleEventsForBackgroundURLSession:completionHandler:[1]
        swiftPMImport.emptyxcode/handleOpenURL|FIRAnalyticsMeta.objc:handleOpenURL:[1]
        swiftPMImport.emptyxcode/handleUserActivity|FIRAnalyticsMeta.objc:handleUserActivity:[1]
        swiftPMImport.emptyxcode/initiateOnDeviceConversionMeasurementWithEmailAddress|FIRAnalyticsMeta.objc:initiateOnDeviceConversionMeasurementWithEmailAddress:[1]
        swiftPMImport.emptyxcode/initiateOnDeviceConversionMeasurementWithHashedEmailAddress|FIRAnalyticsMeta.objc:initiateOnDeviceConversionMeasurementWithHashedEmailAddress:[1]
        swiftPMImport.emptyxcode/initiateOnDeviceConversionMeasurementWithHashedPhoneNumber|FIRAnalyticsMeta.objc:initiateOnDeviceConversionMeasurementWithHashedPhoneNumber:[1]
        swiftPMImport.emptyxcode/initiateOnDeviceConversionMeasurementWithPhoneNumber|FIRAnalyticsMeta.objc:initiateOnDeviceConversionMeasurementWithPhoneNumber:[1]
        swiftPMImport.emptyxcode/setConsent|FIRAnalyticsMeta.objc:setConsent:[1]
    """.trimIndent(),
        ),
        ktSnippet = """
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun createFirestore(): swiftPMImport.emptyxcode.FIRFirestore {
                return swiftPMImport.emptyxcode.FIRFirestore.Companion.firestore()
            }
        """.trimIndent(),
        swiftSnippet = """
            import FirebaseCore
            import FirebaseFirestore
            import Shared

            @main
            struct iOSApp {
                static func main() {
                    let opts = FirebaseOptions(googleAppID: "1:1234567890:ios:abcdef123456", gcmSenderID: "1234567890")
                    opts.apiKey = "AIzaSyDrandomKeyGeneratedForDebug001234"
                    opts.projectID = "dummy"
                    FirebaseApp.configure(options: opts)

                    TempKt.createFirestore().collection("users").document("local_user")
                        .setData(["name": "John Doe", "isOffline": true])
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) { _ ->
        swiftPackage(
            url = url("https://github.com/firebase/firebase-ios-sdk.git"),
            version = exact("12.5.0"),
            products = listOf(product("FirebaseAnalytics"), product("FirebaseFirestore")),
        )
        swiftPackage(
            url = url("https://github.com/apple/swift-protobuf.git"),
            version = exact("1.32.0"),
            products = listOf(product("SwiftProtobuf")),
        )
    }

    @DisplayName("direct dependency on GoogleMaps")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on GoogleMaps`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "provideAPIKey" to """
        swiftPMImport.emptyxcode/GMSServicesMeta.provideAPIKey|objc:provideAPIKey:[1]
    """.trimIndent(),
        ),
        swiftSnippet = """
            import GoogleMaps
            import Shared

            @main
            struct iOSApp {
                static func main() { 
                    GMSServices.provideAPIKey("APIKEY")
                    GMSMapView.map(withFrame: .zero, camera: TempKt.googleMapsCameraPosition())
                }
            }
        """.trimIndent(),
        ktSnippet = """
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun googleMapsCameraPosition(): swiftPMImport.emptyxcode.GMSCameraPosition {
                swiftPMImport.emptyxcode.GMSServices.provideAPIKey("API_KEY")
                val cameraPosition = swiftPMImport.emptyxcode.GMSCameraPosition(latitude = 47.6089945, longitude = -122.3410462, zoom = 14F)
                return cameraPosition
            }
        """.trimIndent(),
        isStatic = isStatic
    ) { _ ->
        iosMinimumDeploymentTarget.set("16.0")
        swiftPackage(
            url = url("https://github.com/googlemaps/ios-maps-sdk.git"),
            version = exact("10.6.0"),
            products = listOf(product("GoogleMaps")),
        )
    }

    @DisplayName("direct dependency on Sentry")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on Sentry`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "startWithConfigureOptions" to """
        swiftPMImport.emptyxcode/SentrySDKMeta.startWithConfigureOptions|objc:startWithConfigureOptions:[1]
    """.trimIndent(),
        ),
        ktSnippet = """
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun sentryTest(): swiftPMImport.emptyxcode.SentryId {
                swiftPMImport.emptyxcode.SentrySDK.startWithConfigureOptions{ options ->
                    options?.setDsn("")
                    options?.setDebug(true)
                }
                return swiftPMImport.emptyxcode.SentrySDK.captureMessage("Hello from iOS!")
            }
        """.trimIndent(),
        swiftSnippet = """
            import Sentry
            import Shared

            @main
            struct iOSApp {
                static func main() {
                    TempKt.sentryTest()
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) { _ ->
        swiftPackage(
            url = url("https://github.com/getsentry/sentry-cocoa.git"),
            version = exact("9.0.0-rc.1"), // use rc to get the fix: https://github.com/getsentry/sentry-cocoa/pull/6607
            products = listOf(product(if (isStatic) "Sentry" else "Sentry-Dynamic")),
        )
    }

    @DisplayName("direct dependency on RevenueCat")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on RevenueCat`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "purchaseProduct" to """
        swiftPMImport.emptyxcode/RCPurchases.purchaseProduct|objc:purchaseProduct:withCompletion:[1]
        swiftPMImport.emptyxcode/RCPurchases.purchaseProduct|objc:purchaseProduct:withPromotionalOffer:completion:[1]
        swiftPMImport.emptyxcode/RCPurchasesTypeProtocol.purchaseProduct|objc:purchaseProduct:withCompletion:[1]
        swiftPMImport.emptyxcode/RCPurchasesTypeProtocol.purchaseProduct|objc:purchaseProduct:withPromotionalOffer:completion:[1]
        swiftPMImport.emptyxcode/purchaseProduct|RCPurchases.objc:purchaseProduct:withCompletion:[1]
        swiftPMImport.emptyxcode/purchaseProduct|RCPurchases.objc:purchaseProduct:withCompletionBlock:[1]
        swiftPMImport.emptyxcode/purchaseProduct|RCPurchases.objc:purchaseProduct:withDiscount:completionBlock:[1]
        swiftPMImport.emptyxcode/purchaseProduct|RCPurchases.objc:purchaseProduct:withPromotionalOffer:completion:[1]
    """.trimIndent(),
        ),
        ktSnippet = """
            import swiftPMImport.emptyxcode.configureWithAPIKey
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun revcat(): String {
                swiftPMImport.emptyxcode.RCPurchases.configureWithAPIKey("dummy")
                val purchases = swiftPMImport.emptyxcode.RCPurchases.sharedPurchases()
                return purchases.appUserID()
            }
        """.trimIndent(),
        swiftSnippet = """
            import RevenueCat
            import Shared

            @main
            struct iOSApp {
                static func main() {
                    let ktUserId = TempKt.revcat()
                    let localUserId = Purchases.shared.appUserID
                    print(ktUserId == localUserId)
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) { _ ->
        swiftPackage(
            url = url("https://github.com/RevenueCat/purchases-ios-spm.git"),
            version = exact("5.49.0"),
            products = listOf(product("RevenueCat")),
        )
    }

    @DisplayName("direct dependency on AWS")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on AWS`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "defaultEC2" to """
        swiftPMImport.emptyxcode/AWSEC2Meta.defaultEC2|objc:defaultEC2[1]
    """.trimIndent(),
            "AWSEC2DescribeInstancesRequest" to """
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.<init>|objc:init#Constructor[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.<init>|objc:initWithCoder:#Constructor[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.<init>|objc:initWithDictionary:error:#Constructor[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.Companion|null[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.dryRun.<get-dryRun>|<get-dryRun>(){}[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.dryRun.<set-dryRun>|<set-dryRun>(<set-?>:platform.Foundation.NSNumber?){}[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.dryRun|objc:dryRun[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.dryRun|{}dryRun[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.filters.<get-filters>|<get-filters>(){}[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.filters.<set-filters>|<set-filters>(<set-?>:kotlin.collections.List<*>?){}[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.filters|objc:filters[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.filters|{}filters[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.initWithCoder|objc:initWithCoder:[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.initWithDictionary|objc:initWithDictionary:error:[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.init|objc:init[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.instanceIds.<get-instanceIds>|<get-instanceIds>(){}[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.instanceIds.<set-instanceIds>|<set-instanceIds>(<set-?>:kotlin.collections.List<*>?){}[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.instanceIds|objc:instanceIds[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.instanceIds|{}instanceIds[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.maxResults.<get-maxResults>|<get-maxResults>(){}[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.maxResults.<set-maxResults>|<set-maxResults>(<set-?>:platform.Foundation.NSNumber?){}[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.maxResults|objc:maxResults[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.maxResults|{}maxResults[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.nextToken.<get-nextToken>|<get-nextToken>(){}[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.nextToken.<set-nextToken>|<set-nextToken>(<set-?>:kotlin.String?){}[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.nextToken|objc:nextToken[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.nextToken|{}nextToken[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.setDryRun|objc:setDryRun:[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.setFilters|objc:setFilters:[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.setInstanceIds|objc:setInstanceIds:[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.setMaxResults|objc:setMaxResults:[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest.setNextToken|objc:setNextToken:[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequestMeta.<init>|<init>(){}[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequestMeta.allocWithZone|objc:allocWithZone:[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequestMeta.alloc|objc:alloc[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequestMeta.modelWithDictionary|objc:modelWithDictionary:error:[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequestMeta.new|objc:new[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequestMeta|null[1]
        swiftPMImport.emptyxcode/AWSEC2DescribeInstancesRequest|null[1]
    """.trimIndent(),
            "AWSEC2ImageAttributeName" to """
        swiftPMImport.emptyxcode/AWSEC2DescribeImageAttributeRequest.attribute.<set-attribute>|<set-attribute>(<set-?>:swiftPMImport.emptyxcode.AWSEC2ImageAttributeName){}[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBlockDeviceMapping|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBootMode|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDeregistrationProtection|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDescription|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameImdsSupport|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameKernel|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLastLaunchedTime|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLaunchPermission|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameProductCodes|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameRAMDisk|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameSriovNetSupport|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameTpmSupport|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUefiData|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUnknown|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.Companion.byValue|byValue(kotlin.Long){}[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.Companion|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.Var.<init>|<init>(kotlin.native.internal.NativePtr){}[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.Var.Companion|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.Var.value.<get-value>|<get-value>(){}[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.Var.value.<set-value>|<set-value>(swiftPMImport.emptyxcode.AWSEC2ImageAttributeName){}[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.Var.value|{}value[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.Var|null[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.value.<get-value>|<get-value>(){}[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName.value|{}value[1]
        swiftPMImport.emptyxcode/AWSEC2ImageAttributeName|null[1]
    """.trimIndent(),
            "AWSS3TransferUtilityDownloadTask" to """
        swiftPMImport.emptyxcode/AWSS3TransferUtilityDownloadTask.<init>|objc:init#Constructor[1]
        swiftPMImport.emptyxcode/AWSS3TransferUtilityDownloadTask.Companion|null[1]
        swiftPMImport.emptyxcode/AWSS3TransferUtilityDownloadTask.init|objc:init[1]
        swiftPMImport.emptyxcode/AWSS3TransferUtilityDownloadTask.setCompletionHandler|objc:setCompletionHandler:[1]
        swiftPMImport.emptyxcode/AWSS3TransferUtilityDownloadTask.setProgressBlock|objc:setProgressBlock:[1]
        swiftPMImport.emptyxcode/AWSS3TransferUtilityDownloadTaskMeta.<init>|<init>(){}[1]
        swiftPMImport.emptyxcode/AWSS3TransferUtilityDownloadTaskMeta.allocWithZone|objc:allocWithZone:[1]
        swiftPMImport.emptyxcode/AWSS3TransferUtilityDownloadTaskMeta.alloc|objc:alloc[1]
        swiftPMImport.emptyxcode/AWSS3TransferUtilityDownloadTaskMeta.new|objc:new[1]
        swiftPMImport.emptyxcode/AWSS3TransferUtilityDownloadTaskMeta|null[1]
        swiftPMImport.emptyxcode/AWSS3TransferUtilityDownloadTask|null[1]
    """.trimIndent(),
        ),
        ktSnippet = """
            import swiftPMImport.emptyxcode.AWSRegionType
            import swiftPMImport.emptyxcode.AWSServiceConfiguration
            import swiftPMImport.emptyxcode.AWSStaticCredentialsProvider
            
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun aws(regionType: AWSRegionType, credentialsProvider: AWSStaticCredentialsProvider?): String {
                val configuration = AWSServiceConfiguration(
                    region = regionType,
                    credentialsProvider = credentialsProvider
                )
                if (configuration.regionType == AWSRegionType.AWSRegionEUNorth1) return "OK"
                return "NOT OK"
            }
        """.trimIndent(),
        // the `EPMIAWSRegionType` is a synthetic re-exported enum from cinterop, the name depends on project name
        swiftSnippet = """
            import AWSCore
            import Shared

            @main
            struct iOSApp {
                static func main() {
                    let result = TempKt.aws(regionType: EPMIAWSRegionType.awsregioneunorth1,
                    credentialsProvider: AWSStaticCredentialsProvider(accessKey: "accessKey", secretKey: "secretKey"))
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) { _ ->
        swiftPackage(
            url = url("https://github.com/aws-amplify/aws-sdk-ios-spm.git"),
            version = exact("2.41.0"),
            products = listOf(product("AWSS3"), product("AWSEC2")),
        )
    }

    @DisplayName("direct dependency on Mapbox")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on Mapbox`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "MapView" to """
        swiftPMImport.emptyxcode/MapView.<init>|objc:init#Constructor[1]
        swiftPMImport.emptyxcode/MapView.<init>|objc:initWithCoder:#Constructor[1]
        swiftPMImport.emptyxcode/MapView.<init>|objc:initWithFrame:#Constructor[1]
        swiftPMImport.emptyxcode/MapView.Companion|null[1]
        swiftPMImport.emptyxcode/MapView.awakeFromNib|objc:awakeFromNib[1]
        swiftPMImport.emptyxcode/MapView.initWithCoder|objc:initWithCoder:[1]
        swiftPMImport.emptyxcode/MapView.initWithFrame|objc:initWithFrame:[1]
        swiftPMImport.emptyxcode/MapView.init|objc:init[1]
        swiftPMImport.emptyxcode/MapViewMeta.<init>|<init>(){}[1]
        swiftPMImport.emptyxcode/MapViewMeta.allocWithZone|objc:allocWithZone:[1]
        swiftPMImport.emptyxcode/MapViewMeta.alloc|objc:alloc[1]
        swiftPMImport.emptyxcode/MapViewMeta.appearanceForTraitCollection|objc:appearanceForTraitCollection:[1]
        swiftPMImport.emptyxcode/MapViewMeta.appearanceForTraitCollection|objc:appearanceForTraitCollection:whenContainedIn:[1]
        swiftPMImport.emptyxcode/MapViewMeta.appearanceForTraitCollection|objc:appearanceForTraitCollection:whenContainedInInstancesOfClasses:[1]
        swiftPMImport.emptyxcode/MapViewMeta.appearanceWhenContainedInInstancesOfClasses|objc:appearanceWhenContainedInInstancesOfClasses:[1]
        swiftPMImport.emptyxcode/MapViewMeta.appearanceWhenContainedIn|objc:appearanceWhenContainedIn:[1]
        swiftPMImport.emptyxcode/MapViewMeta.appearance|objc:appearance[1]
        swiftPMImport.emptyxcode/MapViewMeta.new|objc:new[1]
        swiftPMImport.emptyxcode/MapViewMeta|null[1]
        swiftPMImport.emptyxcode/MapView|null[1]
    """.trimIndent(),
        ),
        ktSnippet = """
            fun mapboxZoom() = 2F
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun mapbox(view: platform.UIKit.UIView) {
                val mapView = swiftPMImport.emptyxcode.MapView(view.bounds) // usage in swift app will fail at runtime
                mapView.addSubview(view)
            }
        """.trimIndent(),
        swiftSnippet = """
            import MapboxMaps
            import Shared

            @main
            struct iOSApp {
                static func main() {
                    let center = CLLocationCoordinate2D(latitude: 39.5, longitude: -98.0)
                    Map(initialViewport: .camera(center: center, zoom: CGFloat(TempKt.mapboxZoom()), bearing: 0, pitch: 0))
                                    .ignoresSafeArea()
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) { _ ->
        swiftPackage(
            url = url("https://github.com/mapbox/mapbox-maps-ios.git"),
            version = exact("11.16.6"),
            products = listOf(product("MapboxMaps")),
        )
    }

    @DisplayName("direct dependency on Tun2SocksKit")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on Tun2SocksKit`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "socks5_tunnel" to """
        swiftPMImport.emptyxcode/hev_socks5_tunnel_main_from_file|hev_socks5_tunnel_main_from_file(kotlin.String?;kotlin.Int){}[1]
        swiftPMImport.emptyxcode/hev_socks5_tunnel_main_from_str|hev_socks5_tunnel_main_from_str(kotlinx.cinterop.CValuesRef<kotlinx.cinterop.UByteVarOf<kotlin.UByte>>?;kotlin.UInt;kotlin.Int){}[1]
        swiftPMImport.emptyxcode/hev_socks5_tunnel_main|hev_socks5_tunnel_main(kotlin.String?;kotlin.Int){}[1]
        swiftPMImport.emptyxcode/hev_socks5_tunnel_quit|hev_socks5_tunnel_quit(){}[1]
        swiftPMImport.emptyxcode/hev_socks5_tunnel_stats|hev_socks5_tunnel_stats(kotlinx.cinterop.CValuesRef<kotlinx.cinterop.ULongVarOf<kotlin.ULong>>?;kotlinx.cinterop.CValuesRef<kotlinx.cinterop.ULongVarOf<kotlin.ULong>>?;kotlinx.cinterop.CValuesRef<kotlinx.cinterop.ULongVarOf<kotlin.ULong>>?;kotlinx.cinterop.CValuesRef<kotlinx.cinterop.ULongVarOf<kotlin.ULong>>?){}[1]
    """.trimIndent(),
        ),
        ktSnippet = """
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun tunnel(): Int{
                val config = ""${'"'}
                    tunnel:
                      mtu: 9000
                    socks5:
                      port: 1080
                      address: 127.0.0.1
                      udp: 'udp'
                    ""${'"'}
                return swiftPMImport.emptyxcode.hev_socks5_tunnel_main(
                    config, 0
                )
            }
        """.trimIndent(),
        swiftSnippet = """
            import Shared
            import Tun2SocksKit

            @main
            struct iOSApp {
                static func main() {
                    TempKt.tunnel()
                    Socks5Tunnel.stats.up.packets
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) { _ ->
        swiftPackage(
            url = url("https://github.com/EbrahimTahernejad/Tun2SocksKit.git"),
            version = exact("5.14.1"),
            products = listOf(product("Tun2SocksKit"))
        )
    }

    @DisplayName("direct dependency on Datadog")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on Datadog`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "Datadog" to """
        swiftPMImport.emptyxcode/DDDatadog.<init>|objc:init#Constructor[1]
        swiftPMImport.emptyxcode/DDDatadog.Companion|null[1]
        swiftPMImport.emptyxcode/DDDatadog.init|objc:init[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.<init>|<init>(){}[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.addAccountExtraInfo|objc:addAccountExtraInfo:[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.addUserExtraInfo|objc:addUserExtraInfo:[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.allocWithZone|objc:allocWithZone:[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.alloc|objc:alloc[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.clearAccountInfo|objc:clearAccountInfo[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.clearAllData|objc:clearAllData[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.clearUserInfo|objc:clearUserInfo[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.initializeWithConfiguration|objc:initializeWithConfiguration:trackingConsent:[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.isInitialized|objc:isInitialized[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.new|objc:new[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.setAccountInfoWithAccountId|objc:setAccountInfoWithAccountId:name:extraInfo:[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.setTrackingConsentWithConsent|objc:setTrackingConsentWithConsent:[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.setUserInfoWithUserId|objc:setUserInfoWithUserId:name:email:extraInfo:[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.setVerbosityLevel|objc:setVerbosityLevel:[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.stopInstance|objc:stopInstance[1]
        swiftPMImport.emptyxcode/DDDatadogMeta.verbosityLevel|objc:verbosityLevel[1]
        swiftPMImport.emptyxcode/DDDatadogMeta|null[1]
        swiftPMImport.emptyxcode/DDDatadog|null[1]
    """.trimIndent(),
            "DDLogs" to """
        swiftPMImport.emptyxcode/DDLogs.<init>|objc:init#Constructor[1]
        swiftPMImport.emptyxcode/DDLogs.Companion|null[1]
        swiftPMImport.emptyxcode/DDLogs.init|objc:init[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration.<init>|objc:init#Constructor[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration.<init>|objc:initWithCustomEndpoint:#Constructor[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration.Companion|null[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration.customEndpoint.<get-customEndpoint>|<get-customEndpoint>(){}[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration.customEndpoint.<set-customEndpoint>|<set-customEndpoint>(platform.Foundation.NSURL?){}[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration.customEndpoint|objc:customEndpoint[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration.customEndpoint|{}customEndpoint[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration.initWithCustomEndpoint|objc:initWithCustomEndpoint:[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration.init|objc:init[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration.setCustomEndpoint|objc:setCustomEndpoint:[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration.setEventMapper|objc:setEventMapper:[1]
        swiftPMImport.emptyxcode/DDLogsConfigurationMeta.<init>|<init>(){}[1]
        swiftPMImport.emptyxcode/DDLogsConfigurationMeta.allocWithZone|objc:allocWithZone:[1]
        swiftPMImport.emptyxcode/DDLogsConfigurationMeta.alloc|objc:alloc[1]
        swiftPMImport.emptyxcode/DDLogsConfigurationMeta.new|objc:new[1]
        swiftPMImport.emptyxcode/DDLogsConfigurationMeta|null[1]
        swiftPMImport.emptyxcode/DDLogsConfiguration|null[1]
        swiftPMImport.emptyxcode/DDLogsMeta.<init>|<init>(){}[1]
        swiftPMImport.emptyxcode/DDLogsMeta.addAttributeForKey|objc:addAttributeForKey:value:[1]
        swiftPMImport.emptyxcode/DDLogsMeta.allocWithZone|objc:allocWithZone:[1]
        swiftPMImport.emptyxcode/DDLogsMeta.alloc|objc:alloc[1]
        swiftPMImport.emptyxcode/DDLogsMeta.enableWith|objc:enableWith:[1]
        swiftPMImport.emptyxcode/DDLogsMeta.new|objc:new[1]
        swiftPMImport.emptyxcode/DDLogsMeta.removeAttributeForKey|objc:removeAttributeForKey:[1]
        swiftPMImport.emptyxcode/DDLogsMeta|null[1]
        swiftPMImport.emptyxcode/DDLogs|null[1]
    """.trimIndent(),
        ),
        ktSnippet = """
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun ddogInit() {
                val configuration = swiftPMImport.emptyxcode.DDConfiguration("dummy_client_token", "unit_test_env")
                configuration.setService("org.test")
                swiftPMImport.emptyxcode.DDDatadog.initializeWithConfiguration(configuration, swiftPMImport.emptyxcode.DDTrackingConsent.granted())
            }
        """.trimIndent(),
        swiftSnippet = """
            import Shared
            import DatadogCore

            @main
            struct iOSApp {
                static func main() {
                    TempKt.ddogInit()
                    let isInitialized = Datadog.isInitialized()
                    if(!isInitialized) { fatalError("DD should be initiated in K/N") }
                    print("Is Initialized: \(isInitialized)")
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) { _ ->
        swiftPackage(
            url = url("https://github.com/DataDog/dd-sdk-ios.git"),
            version = exact("3.3.0"),
            products = listOf(product("DatadogCore"), product("DatadogLogs")),
        )
    }

    @DisplayName("direct dependency on AdjustSDK")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on AdjustSDK`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "WKWebView" to """
        swiftPMImport.emptyxcode/AdjustBridge.loadWKWebViewBridge|objc:loadWKWebViewBridge:[1]
        swiftPMImport.emptyxcode/AdjustBridge.wkWebView.<set-wkWebView>|<set-wkWebView>(platform.WebKit.WKWebView){}[1]
    """.trimIndent(),
            "ADJConfig" to """
        swiftPMImport.emptyxcode/ADJConfig.<init>|objc:init#Constructor[1]
        swiftPMImport.emptyxcode/ADJConfig.<init>|objc:initWithAppToken:environment:#Constructor[1]
        swiftPMImport.emptyxcode/ADJConfig.<init>|objc:initWithAppToken:environment:suppressLogLevel:#Constructor[1]
        swiftPMImport.emptyxcode/ADJConfig.Companion|null[1]
        swiftPMImport.emptyxcode/ADJConfig.appToken.<get-appToken>|<get-appToken>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.appToken|objc:appToken[1]
        swiftPMImport.emptyxcode/ADJConfig.appToken|{}appToken[1]
        swiftPMImport.emptyxcode/ADJConfig.attConsentWaitingInterval.<get-attConsentWaitingInterval>|<get-attConsentWaitingInterval>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.attConsentWaitingInterval.<set-attConsentWaitingInterval>|<set-attConsentWaitingInterval>(kotlin.ULong){}[1]
        swiftPMImport.emptyxcode/ADJConfig.attConsentWaitingInterval|objc:attConsentWaitingInterval[1]
        swiftPMImport.emptyxcode/ADJConfig.attConsentWaitingInterval|{}attConsentWaitingInterval[1]
        swiftPMImport.emptyxcode/ADJConfig.copyWithZone|objc:copyWithZone:[1]
        swiftPMImport.emptyxcode/ADJConfig.defaultTracker.<get-defaultTracker>|<get-defaultTracker>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.defaultTracker.<set-defaultTracker>|<set-defaultTracker>(kotlin.String?){}[1]
        swiftPMImport.emptyxcode/ADJConfig.defaultTracker|objc:defaultTracker[1]
        swiftPMImport.emptyxcode/ADJConfig.defaultTracker|{}defaultTracker[1]
        swiftPMImport.emptyxcode/ADJConfig.delegate.<get-delegate>|<get-delegate>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.delegate.<set-delegate>|<set-delegate>(platform.darwin.NSObject?){}[1]
        swiftPMImport.emptyxcode/ADJConfig.delegate|objc:delegate[1]
        swiftPMImport.emptyxcode/ADJConfig.delegate|{}delegate[1]
        swiftPMImport.emptyxcode/ADJConfig.disableAdServices|objc:disableAdServices[1]
        swiftPMImport.emptyxcode/ADJConfig.disableAppTrackingTransparencyUsage|objc:disableAppTrackingTransparencyUsage[1]
        swiftPMImport.emptyxcode/ADJConfig.disableIdfaReading|objc:disableIdfaReading[1]
        swiftPMImport.emptyxcode/ADJConfig.disableIdfvReading|objc:disableIdfvReading[1]
        swiftPMImport.emptyxcode/ADJConfig.disableSkanAttribution|objc:disableSkanAttribution[1]
        swiftPMImport.emptyxcode/ADJConfig.enableCoppaCompliance|objc:enableCoppaCompliance[1]
        swiftPMImport.emptyxcode/ADJConfig.enableCostDataInAttribution|objc:enableCostDataInAttribution[1]
        swiftPMImport.emptyxcode/ADJConfig.enableDeviceIdsReadingOnce|objc:enableDeviceIdsReadingOnce[1]
        swiftPMImport.emptyxcode/ADJConfig.enableFirstSessionDelay|objc:enableFirstSessionDelay[1]
        swiftPMImport.emptyxcode/ADJConfig.enableLinkMe|objc:enableLinkMe[1]
        swiftPMImport.emptyxcode/ADJConfig.enableSendingInBackground|objc:enableSendingInBackground[1]
        swiftPMImport.emptyxcode/ADJConfig.environment.<get-environment>|<get-environment>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.environment|objc:environment[1]
        swiftPMImport.emptyxcode/ADJConfig.environment|{}environment[1]
        swiftPMImport.emptyxcode/ADJConfig.eventDeduplicationIdsMaxSize.<get-eventDeduplicationIdsMaxSize>|<get-eventDeduplicationIdsMaxSize>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.eventDeduplicationIdsMaxSize.<set-eventDeduplicationIdsMaxSize>|<set-eventDeduplicationIdsMaxSize>(kotlin.Long){}[1]
        swiftPMImport.emptyxcode/ADJConfig.eventDeduplicationIdsMaxSize|objc:eventDeduplicationIdsMaxSize[1]
        swiftPMImport.emptyxcode/ADJConfig.eventDeduplicationIdsMaxSize|{}eventDeduplicationIdsMaxSize[1]
        swiftPMImport.emptyxcode/ADJConfig.externalDeviceId.<get-externalDeviceId>|<get-externalDeviceId>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.externalDeviceId.<set-externalDeviceId>|<set-externalDeviceId>(kotlin.String?){}[1]
        swiftPMImport.emptyxcode/ADJConfig.externalDeviceId|objc:externalDeviceId[1]
        swiftPMImport.emptyxcode/ADJConfig.externalDeviceId|{}externalDeviceId[1]
        swiftPMImport.emptyxcode/ADJConfig.initWithAppToken|objc:initWithAppToken:environment:[1]
        swiftPMImport.emptyxcode/ADJConfig.initWithAppToken|objc:initWithAppToken:environment:suppressLogLevel:[1]
        swiftPMImport.emptyxcode/ADJConfig.init|objc:init[1]
        swiftPMImport.emptyxcode/ADJConfig.isAdServicesEnabled.<get-isAdServicesEnabled>|<get-isAdServicesEnabled>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isAdServicesEnabled|objc:isAdServicesEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isAdServicesEnabled|{}isAdServicesEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isAppTrackingTransparencyUsageEnabled.<get-isAppTrackingTransparencyUsageEnabled>|<get-isAppTrackingTransparencyUsageEnabled>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isAppTrackingTransparencyUsageEnabled|objc:isAppTrackingTransparencyUsageEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isAppTrackingTransparencyUsageEnabled|{}isAppTrackingTransparencyUsageEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isCoppaComplianceEnabled.<get-isCoppaComplianceEnabled>|<get-isCoppaComplianceEnabled>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isCoppaComplianceEnabled.<set-isCoppaComplianceEnabled>|<set-isCoppaComplianceEnabled>(kotlin.Boolean){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isCoppaComplianceEnabled|objc:isCoppaComplianceEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isCoppaComplianceEnabled|{}isCoppaComplianceEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isCostDataInAttributionEnabled.<get-isCostDataInAttributionEnabled>|<get-isCostDataInAttributionEnabled>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isCostDataInAttributionEnabled|objc:isCostDataInAttributionEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isCostDataInAttributionEnabled|{}isCostDataInAttributionEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isDataResidency.<get-isDataResidency>|<get-isDataResidency>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isDataResidency|objc:isDataResidency[1]
        swiftPMImport.emptyxcode/ADJConfig.isDataResidency|{}isDataResidency[1]
        swiftPMImport.emptyxcode/ADJConfig.isDeviceIdsReadingOnceEnabled.<get-isDeviceIdsReadingOnceEnabled>|<get-isDeviceIdsReadingOnceEnabled>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isDeviceIdsReadingOnceEnabled|objc:isDeviceIdsReadingOnceEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isDeviceIdsReadingOnceEnabled|{}isDeviceIdsReadingOnceEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isFirstSessionDelayEnabled.<get-isFirstSessionDelayEnabled>|<get-isFirstSessionDelayEnabled>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isFirstSessionDelayEnabled|objc:isFirstSessionDelayEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isFirstSessionDelayEnabled|{}isFirstSessionDelayEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isIdfaReadingEnabled.<get-isIdfaReadingEnabled>|<get-isIdfaReadingEnabled>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isIdfaReadingEnabled|objc:isIdfaReadingEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isIdfaReadingEnabled|{}isIdfaReadingEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isIdfvReadingEnabled.<get-isIdfvReadingEnabled>|<get-isIdfvReadingEnabled>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isIdfvReadingEnabled|objc:isIdfvReadingEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isIdfvReadingEnabled|{}isIdfvReadingEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isLinkMeEnabled.<get-isLinkMeEnabled>|<get-isLinkMeEnabled>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isLinkMeEnabled|objc:isLinkMeEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isLinkMeEnabled|{}isLinkMeEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isSendingInBackgroundEnabled.<get-isSendingInBackgroundEnabled>|<get-isSendingInBackgroundEnabled>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isSendingInBackgroundEnabled|objc:isSendingInBackgroundEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isSendingInBackgroundEnabled|{}isSendingInBackgroundEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isSkanAttributionEnabled.<get-isSkanAttributionEnabled>|<get-isSkanAttributionEnabled>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.isSkanAttributionEnabled|objc:isSkanAttributionEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isSkanAttributionEnabled|{}isSkanAttributionEnabled[1]
        swiftPMImport.emptyxcode/ADJConfig.isValid|objc:isValid[1]
        swiftPMImport.emptyxcode/ADJConfig.logLevel.<get-logLevel>|<get-logLevel>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.logLevel.<set-logLevel>|<set-logLevel>(kotlin.ULong){}[1]
        swiftPMImport.emptyxcode/ADJConfig.logLevel|objc:logLevel[1]
        swiftPMImport.emptyxcode/ADJConfig.logLevel|{}logLevel[1]
        swiftPMImport.emptyxcode/ADJConfig.sdkPrefix.<get-sdkPrefix>|<get-sdkPrefix>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.sdkPrefix.<set-sdkPrefix>|<set-sdkPrefix>(kotlin.String?){}[1]
        swiftPMImport.emptyxcode/ADJConfig.sdkPrefix|objc:sdkPrefix[1]
        swiftPMImport.emptyxcode/ADJConfig.sdkPrefix|{}sdkPrefix[1]
        swiftPMImport.emptyxcode/ADJConfig.setAttConsentWaitingInterval|objc:setAttConsentWaitingInterval:[1]
        swiftPMImport.emptyxcode/ADJConfig.setDefaultTracker|objc:setDefaultTracker:[1]
        swiftPMImport.emptyxcode/ADJConfig.setDelegate|objc:setDelegate:[1]
        swiftPMImport.emptyxcode/ADJConfig.setEventDeduplicationIdsMaxSize|objc:setEventDeduplicationIdsMaxSize:[1]
        swiftPMImport.emptyxcode/ADJConfig.setExternalDeviceId|objc:setExternalDeviceId:[1]
        swiftPMImport.emptyxcode/ADJConfig.setIsCoppaComplianceEnabled|objc:setIsCoppaComplianceEnabled:[1]
        swiftPMImport.emptyxcode/ADJConfig.setLogLevel|objc:setLogLevel:[1]
        swiftPMImport.emptyxcode/ADJConfig.setSdkPrefix|objc:setSdkPrefix:[1]
        swiftPMImport.emptyxcode/ADJConfig.setStoreInfo|objc:setStoreInfo:[1]
        swiftPMImport.emptyxcode/ADJConfig.setUrlStrategy|objc:setUrlStrategy:useSubdomains:isDataResidency:[1]
        swiftPMImport.emptyxcode/ADJConfig.storeInfo.<get-storeInfo>|<get-storeInfo>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.storeInfo.<set-storeInfo>|<set-storeInfo>(swiftPMImport.emptyxcode.ADJStoreInfo){}[1]
        swiftPMImport.emptyxcode/ADJConfig.storeInfo|objc:storeInfo[1]
        swiftPMImport.emptyxcode/ADJConfig.storeInfo|{}storeInfo[1]
        swiftPMImport.emptyxcode/ADJConfig.urlStrategyDomains.<get-urlStrategyDomains>|<get-urlStrategyDomains>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.urlStrategyDomains|objc:urlStrategyDomains[1]
        swiftPMImport.emptyxcode/ADJConfig.urlStrategyDomains|{}urlStrategyDomains[1]
        swiftPMImport.emptyxcode/ADJConfig.useSubdomains.<get-useSubdomains>|<get-useSubdomains>(){}[1]
        swiftPMImport.emptyxcode/ADJConfig.useSubdomains|objc:useSubdomains[1]
        swiftPMImport.emptyxcode/ADJConfig.useSubdomains|{}useSubdomains[1]
        swiftPMImport.emptyxcode/ADJConfigMeta.<init>|<init>(){}[1]
        swiftPMImport.emptyxcode/ADJConfigMeta.allocWithZone|objc:allocWithZone:[1]
        swiftPMImport.emptyxcode/ADJConfigMeta.alloc|objc:alloc[1]
        swiftPMImport.emptyxcode/ADJConfigMeta.new|objc:new[1]
        swiftPMImport.emptyxcode/ADJConfigMeta|null[1]
        swiftPMImport.emptyxcode/ADJConfig|null[1]
    """.trimIndent(),
        ),
        ktSnippet = """
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun adjustConfig() = swiftPMImport.emptyxcode.ADJConfig(appToken = "token", environment = "env")
        """.trimIndent(),
        swiftSnippet = """
            import Shared
            import AdjustSdk

            @main
            struct iOSApp {
                static func main() {
                    let config = TempKt.adjustConfig()
                    Adjust.initSdk(config)
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) { _ ->
        swiftPackage(
            url = url("https://github.com/adjust/ios_sdk.git"),
            version = exact("5.4.6"),
            products = listOf(product("AdjustWebBridge"))
        )
    }

    @DisplayName("direct dependency on AppAuth")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on AppAuth`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "initWithAuthorizationEndpoint" to """
        swiftPMImport.emptyxcode/OIDServiceConfiguration.<init>|objc:initWithAuthorizationEndpoint:tokenEndpoint:#Constructor[1]
        swiftPMImport.emptyxcode/OIDServiceConfiguration.<init>|objc:initWithAuthorizationEndpoint:tokenEndpoint:issuer:#Constructor[1]
        swiftPMImport.emptyxcode/OIDServiceConfiguration.<init>|objc:initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:#Constructor[1]
        swiftPMImport.emptyxcode/OIDServiceConfiguration.<init>|objc:initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:endSessionEndpoint:#Constructor[1]
        swiftPMImport.emptyxcode/OIDServiceConfiguration.<init>|objc:initWithAuthorizationEndpoint:tokenEndpoint:registrationEndpoint:#Constructor[1]
        swiftPMImport.emptyxcode/OIDServiceConfiguration.initWithAuthorizationEndpoint|objc:initWithAuthorizationEndpoint:tokenEndpoint:[1]
        swiftPMImport.emptyxcode/OIDServiceConfiguration.initWithAuthorizationEndpoint|objc:initWithAuthorizationEndpoint:tokenEndpoint:issuer:[1]
        swiftPMImport.emptyxcode/OIDServiceConfiguration.initWithAuthorizationEndpoint|objc:initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:[1]
        swiftPMImport.emptyxcode/OIDServiceConfiguration.initWithAuthorizationEndpoint|objc:initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:endSessionEndpoint:[1]
        swiftPMImport.emptyxcode/OIDServiceConfiguration.initWithAuthorizationEndpoint|objc:initWithAuthorizationEndpoint:tokenEndpoint:registrationEndpoint:[1]
    """.trimIndent(),
        ),
        ktSnippet = """
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun auth(): swiftPMImport.emptyxcode.OIDServiceConfiguration {
                val authEndpoint = platform.Foundation.NSURL.URLWithString("https://example.com/oauth2/auth")
                val tokenEndpoint = platform.Foundation.NSURL.URLWithString("https://example.com/oauth2/token")
                return swiftPMImport.emptyxcode.OIDServiceConfiguration(authEndpoint!!, tokenEndpoint!!)
            }
        """.trimIndent(),
        swiftSnippet = """
            import Shared
            import AppAuth

            @main
            struct iOSApp {
                static func main() {
                    let request = OIDAuthorizationRequest.init(configuration: TempKt.auth(), clientId: "", scopes: [], redirectURL: NSURL(string: "http://localhost") as! URL, responseType: "code", additionalParameters: ["": ""])
                    let url = request.authorizationRequestURL().absoluteString
                    if(!url.contains("example.com")) { fatalError("value from kotlin is not passed") }
                    print(url)
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) { _ ->
        swiftPackage(
            url = url("https://github.com/openid/AppAuth-iOS.git"),
            version = exact("2.0.0"),
            products = listOf(product("AppAuth"))
        )
    }

    @DisplayName("local SwiftPM package with relative path")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `local SwiftPM package with relative path`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "greeting" to """
        swiftPMImport.emptyxcode/LocalHelperMeta.greeting|objc:greeting[1]
    """.trimIndent(),
        ),
        ktSnippet = """
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun localGreeting(): String {
                return swiftPMImport.emptyxcode.LocalHelper.greeting()
            }
        """.trimIndent(),
        swiftSnippet = """
            import LocalSwiftPackage
            import Shared

            @main
            struct iOSApp {
                static func main() {
                    let ktGreeting = TempKt.localGreeting()
                    let swiftGreeting = LocalHelper.greeting()
                    print("Match: \(ktGreeting == swiftGreeting)")
                }
            }
        """.trimIndent(),
        isStatic = isStatic,
        // No lock file is created
        synchronizeLockFileWithXcodeProject = false,
        expectedPackageManifest = if (isStatic) {
            """
                // swift-tools-version: 5.9
                import PackageDescription
                let package = Package(
                  name: "$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME",
                  platforms: [
                    .iOS("15.0")
                  ],
                  products: [
                    .library(
                      name: "$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME",
                      type: .none,
                      targets: ["$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME"]
                    )
                  ],
                  dependencies: [
                    .package(
                      path: "../../../localSwiftPackage"
                    )
                  ],
                  targets: [
                    .target(
                      name: "$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME",
                      dependencies: [
                        .product(
                          name: "LocalSwiftPackage",
                          package: "localSwiftPackage"
                        )
                      ]
                    )
                  ]
                )
            """.trimIndent() + "\n"
        } else null,
        beforeBuild = {
            // Generate local Swift package as a sibling directory (to test relative path with ../)
            val localPackageDir = projectPath.resolve("../localSwiftPackage")
            localPackageDir.resolve("Sources/LocalSwiftPackage").createDirectories()

            localPackageDir.resolve("Package.swift").writeText(
                """
                // swift-tools-version: 5.9
                import PackageDescription

                let package = Package(
                    name: "LocalSwiftPackage",
                    platforms: [.iOS(.v15)],
                    products: [
                        .library(name: "LocalSwiftPackage", targets: ["LocalSwiftPackage"]),
                    ],
                    targets: [
                        .target(name: "LocalSwiftPackage"),
                    ]
                )
                """.trimIndent()
            )

            localPackageDir.resolve("Sources/LocalSwiftPackage/LocalSwiftPackage.swift").writeText(
                """
                import Foundation

                @objc public class LocalHelper: NSObject {
                    @objc public static func greeting() -> String {
                        return "Hello from LocalSwiftPackage"
                    }
                }
                """.trimIndent()
            )
        }
    ) { layout ->
        localSwiftPackage(
            directory = layout.projectDirectory.dir("../localSwiftPackage"),
            products = listOf("LocalSwiftPackage"),
        )
    }

    /**
     * Tests multiproject setup with local SwiftPM dependency.
     *
     * Project structure:
     * ```
     * emptyxcode (root consumer)
     * ├── iosApp/                          <- Xcode app (uses root's framework)
     * ├── producer/
     * │   ├── localSwiftPackage/           <- Local Swift Package
     * │   │   ├── Package.swift
     * │   │   └── Sources/LocalSwiftPackage/LocalSwiftPackage.swift
     * │   └── build.gradle.kts             <- swiftPMDependencies { localPackage(...) }
     * └── build.gradle.kts                 <- depends on :producer (commonMain)
     *                                         framework binaries (Shared.framework)
     * ```
     *
     * Dependencies:
     * ```
     * ┌──────────────────────────────────────────────────────────────────┐
     * │                        Gradle Dependencies                       │
     * │                                                                  │
     * │   emptyxcode (root) ──────────────────────────► producer         │
     * │       │                 (commonMain)                │            │
     * │       │                                             │            │
     * │       │                                             ▼            │
     * │       │                            ┌─────────────────────────┐   │
     * │       │                            │  SwiftPM Dependencies   │   │
     * │       │                            │                         │   │
     * │       │                            │  localSwiftPackage      │   │
     * │       │                            └─────────────────────────┘   │
     * │       │                                                          │
     * │       ▼                                                          │
     * │  Shared.framework                                                │
     * │  (built by root project)                                         │
     * └──────────────────────────────────────────────────────────────────┘
     * ```
     *
     * Producer has:
     * - Local SwiftPM dependency (localSwiftPackage)
     * - iosMain code using swiftPMImport
     *
     * Root project (consumer) has:
     * - commonMain dependency on producer
     * - Framework binaries (Shared.framework)
     * - iosApp uses root's framework for Xcode linkage testing
     */
    @DisplayName("multiproject local SwiftPM dependency with producer and consumer")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `multiproject local SwiftPM dependency with producer and consumer`(
        version: GradleVersion,
        isStatic: Boolean,
    ) {
        project("emptyxcode", version) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(iosArm64(), iosSimulatorArm64()).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            this.isStatic = isStatic
                        }
                    }

                    sourceSets.commonMain {
                        compileSource(
                            """
                                package consumer
                                object Consumer {
                                    fun localGreeting() = producer.localGreeting()
                                }
                            """.trimIndent()
                        )
                        dependencies {
                            implementation(project(":producer"))
                        }
                    }
                }
            }

            // Create producer project with local SwiftPM dependency
            val producer = project("empty", version) {
                // Create local Swift package inside producer project
                val localPackageDir = projectPath.resolve("localSwiftPackage")
                localPackageDir.resolve("Sources/LocalSwiftPackage").createDirectories()

                localPackageDir.resolve("Package.swift").writeText(
                    """
                    // swift-tools-version: 5.9
                    import PackageDescription
                    let package = Package(
                        name: "LocalSwiftPackage",
                        platforms: [.iOS(.v15)],
                        products: [
                            .library(name: "LocalSwiftPackage", targets: ["LocalSwiftPackage"]),
                        ],
                        targets: [
                            .target(name: "LocalSwiftPackage"),
                        ]
                    )
                """.trimIndent()
                )

                localPackageDir.resolve("Sources/LocalSwiftPackage/LocalSwiftPackage.swift").writeText(
                    """
                    import Foundation
                    @objc public class LocalHelper: NSObject {
                        @objc public static func greeting() -> String {
                            return "Hello from LocalSwiftPackage"
                        }
                    }
                """.trimIndent()
                )

                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        iosSimulatorArm64()

                        sourceSets.iosMain.get().compileSource(
                            """
                            @file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                            package producer
                            fun localGreeting(): String {
                                return swiftPMImport.emptyxcode.producer.LocalHelper.greeting()
                            }
                        """.trimIndent()
                        )

                        swiftPMDependencies {
                            localSwiftPackage(
                                directory = project.layout.projectDirectory.dir("localSwiftPackage"),
                                products = listOf("LocalSwiftPackage"),
                            )
                        }
                    }
                }
            }

            include(producer, "producer", useSymlink = false)

            // Update the root iosApp Swift file to use the consumer's framework
            val swiftAppFile = projectPath.resolve("iosApp/iosApp/iOSApp.swift")
            swiftAppFile.writeText(
                """
                import LocalSwiftPackage
                import Shared

                @main
                struct iOSApp {
                    static func main() {
                        let ktGreeting = Consumer.shared.localGreeting()
                        let swiftGreeting = LocalHelper.greeting()
                        print(ktGreeting == swiftGreeting)
                    }
                }
            """.trimIndent()
            )

            // Get producer project path
            // With useSymlink=false, the producer project is copied into the root project.
            val producerPath = projectPath.resolve("producer")

            // Verify cinterop API signatures from producer (same API as local SwiftPM test)
            testVisibleSignatures(
                expectedCinteropAPIs = mapOf(
                    "greeting" to """
        swiftPMImport.emptyxcode.producer/LocalHelperMeta.greeting|objc:greeting[1]
    """.trimIndent(),
                ),
                commonizerBasePath = producerPath,
                commonizeTask = ":producer:commonizeCInterop"
            )

            // Full Kotlin linkage (both release and debug)
            testKotlinLinkage()

            // Xcode linkage
            testXcodeLinkage(projectPath.resolve("dd"))

            // Verify Package.swift in root project with exact content
            // Uses synthetic subpackage reference for the producer dependency
            if (isStatic) {
                testPackageManifest(
                    expectedContent = """
                            // swift-tools-version: 5.9
                            import PackageDescription
                            let package = Package(
                              name: "$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME",
                              platforms: [
                                .iOS("15.0")
                              ],
                              products: [
                                .library(
                                  name: "$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME",
                                  type: .none,
                                  targets: ["$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME"]
                                )
                              ],
                              dependencies: [
                                .package(path: "subpackages/_producer")
                              ],
                              targets: [
                                .target(
                                  name: "$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME",
                                  dependencies: [
                                    .product(name: "_producer", package: "_producer")
                                  ]
                                )
                              ]
                            )
                        """.trimIndent() + "\n"
                )
                // With useSymlink=false, the producer project is copied into the root project at:
                //   projectPath/producer
                // The local Swift package lives under:
                //   projectPath/producer/localSwiftPackage
                testPackageManifest(
                    expectedContent = """
                            // swift-tools-version: 5.9
                            import PackageDescription
                            let package = Package(
                              name: "_producer",
                              platforms: [
                                .iOS("15.0")
                              ],
                              products: [
                                .library(
                                  name: "_producer",
                                  type: .none,
                                  targets: ["_producer"]
                                )
                              ],
                              dependencies: [
                                .package(
                                  path: "../../../../producer/localSwiftPackage"
                                )
                              ],
                              targets: [
                                .target(
                                  name: "_producer",
                                  dependencies: [
                                    .product(
                                      name: "LocalSwiftPackage",
                                      package: "localSwiftPackage"
                                    )
                                  ]
                                )
                              ]
                            )
                        """.trimIndent() + "\n",
                    manifestRelativePath = "iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/subpackages/_producer/Package.swift"
                )
            }
        }
    }

    private fun testSwiftPackageIntegration(
        version: GradleVersion,
        expectedCinteropAPIs: Map<String, String>,
        swiftSnippet: String = "",
        ktSnippet: String = "",
        isStatic: Boolean,
        synchronizeLockFileWithXcodeProject: Boolean = true,
        expectedPackageManifest: String? = null,
        beforeBuild: (TestProject.() -> Unit)? = null,
        configure: SwiftPMImportExtension.(ProjectLayout) -> Unit,
    ) {
        project("emptyxcode", version) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "Shared"
                            this.isStatic = isStatic
                        }
                    }

                    swiftPMDependencies {
                        configure(project.layout)
                    }
                }
            }

            val swiftFile = projectPath.resolve("iosApp/iosApp/iOSApp.swift")
            if (swiftSnippet.isNotBlank()) swiftFile.writeText(swiftSnippet)

            val ktFile = kotlinSourcesDir("iosMain").createDirectories().resolve("temp.kt").createFile()
            ktFile.writeText(ktSnippet)

            beforeBuild?.invoke(this)

            testVisibleSignatures(expectedCinteropAPIs)
            testKotlinLinkage()
            if (synchronizeLockFileWithXcodeProject) {
                copyLockFileIntoIosProject()
            }
            val derivedDataPath = projectPath.resolve("dd")
            testXcodeLinkage(derivedDataPath)
            val appPath = derivedDataPath.resolve("Build/Products/Debug-iphonesimulator/emptyxcode.app")
            assertApplicationRunsAndObjCRuntimeDoesntEmitInStderr(appPath)
            if (expectedPackageManifest != null) {
                testPackageManifest(expectedPackageManifest)
            }
        }
    }

    private class SpmImportArgumentsProvider : GradleArgumentsProvider() {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return super.provideArguments(context).flatMap { arguments ->
                val gradleVersion = arguments.get().first()
                Stream.of(true, false).map { isStatic ->
                    Arguments.of(gradleVersion, isStatic)
                }
            }
        }
    }
}

private fun TestProject.testKotlinLinkage() {
    // build(":linkReleaseFrameworkIosArm64")
    build(":linkDebugFrameworkIosSimulatorArm64")
}

private fun TestProject.copyLockFileIntoIosProject() {
    projectPath.resolve(".swiftpm-locks/default/swiftImport/Package.resolved").copyTo(
        projectPath.resolve("iosApp/iosApp.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved").also {
            it.parent.createDirectories()
        }
    )
}

@OptIn(EnvironmentalVariablesOverride::class)
private fun TestProject.testXcodeLinkage(derivedDataPath: Path) {
    build(
        "integrateLinkagePackage",
        environmentVariables = EnvironmentalVariables(
            "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj"
        )
    )
    buildXcodeProject(
        xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
        derivedDataPath = derivedDataPath,
    )
}

private fun TestProject.testVisibleSignatures(
    expectedCinteropAPIs: Map<String, String>,
    commonizerBasePath: Path = projectPath,
    commonizeTask: String = "commonizeCInterop",
) {
    val metadataDump = commonizeAndDumpCinteropSignatures(commonizerBasePath, commonizeTask)

    val actualSignatures = mutableMapOf<String, MutableList<String>>()
    metadataDump.lines().forEach { line ->
        expectedCinteropAPIs.keys.forEach { key ->
            if (key in line) {
                actualSignatures.getOrPut(key) { mutableListOf() }.add(line)
            }
        }
    }

    assertEquals(
        expectedCinteropAPIs.prettyPrinted,
        actualSignatures.mapValues { it.value.joinToString("\n").trimIndent() }.prettyPrinted,
    )
}

private fun TestProject.testPackageManifest(
    expectedContent: String,
    swiftImportBasePath: Path = projectPath,
    manifestRelativePath: String = "iosApp/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME/Package.swift",
) {
    val packageSwift = swiftImportBasePath.resolve(manifestRelativePath)

    // Verify the file exists
    assert(packageSwift.exists()) { "Package.swift should exist at $packageSwift" }

    val actualContent = packageSwift.readText()
    assertEquals(expectedContent, actualContent, "Package.swift content mismatch")
}

internal fun KotlinMultiplatformExtension.swiftPMDependencies(configure: SwiftPMImportExtension.() -> Unit) {
    (this.extensions.getByName(SwiftPMImportExtension.EXTENSION_NAME) as SwiftPMImportExtension).configure()
}

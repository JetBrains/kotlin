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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.*
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.konan.target.Xcode
import org.jetbrains.kotlin.konan.target.XcodeVersion
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.support.ParameterDeclarations
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
        public open expect fun swiftPMImport/emptyxcode/FIRFirestoreMeta.firestoreForApp(app: swiftPMImport/emptyxcode/FIRApp): swiftPMImport/emptyxcode/FIRFirestore
        public open expect fun swiftPMImport/emptyxcode/FIRFirestoreMeta.firestoreForApp(app: swiftPMImport/emptyxcode/FIRApp, database: kotlin/String): swiftPMImport/emptyxcode/FIRFirestore
    """.trimIndent(),
            "FIRAnalytics" to """
        public open expect class swiftPMImport/emptyxcode/FIRAnalytics : platform/darwin/NSObject
        public /* secondary */ constructor swiftPMImport/emptyxcode/FIRAnalytics.<init>()
        public open expect fun swiftPMImport/emptyxcode/FIRAnalytics.init(): swiftPMImport/emptyxcode/FIRAnalytics?
        public final expect companion object swiftPMImport/emptyxcode/FIRAnalytics.Companion : swiftPMImport/emptyxcode/FIRAnalyticsMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/FIRAnalytics>
        public open expect class swiftPMImport/emptyxcode/FIRAnalyticsMeta : platform/darwin/NSObjectMeta
        protected /* secondary */ constructor swiftPMImport/emptyxcode/FIRAnalyticsMeta.<init>()
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.alloc(): swiftPMImport/emptyxcode/FIRAnalytics?
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/FIRAnalytics?
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.appInstanceID(): kotlin/String?
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.logEventWithName(name: kotlin/String, parameters: kotlin/collections/Map<kotlin/Any?, *>?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.new(): swiftPMImport/emptyxcode/FIRAnalytics?
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.resetAnalyticsData(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.sessionIDWithCompletion(completion: kotlin/Function2<kotlin/Long, platform/Foundation/NSError?, kotlin/Unit>): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.setAnalyticsCollectionEnabled(analyticsCollectionEnabled: kotlin/Boolean): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.setDefaultEventParameters(parameters: kotlin/collections/Map<kotlin/Any?, *>?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.setSessionTimeoutInterval(sessionTimeoutInterval: kotlin/Double): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.setUserID(userID: kotlin/String?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.setUserPropertyString(value: kotlin/String?, forName: kotlin/String): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.swiftPMImport/emptyxcode/handleEventsForBackgroundURLSession(identifier: kotlin/String, completionHandler: kotlin/Function0<kotlin/Unit>?): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.swiftPMImport/emptyxcode/handleOpenURL(url: platform/Foundation/NSURL): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.swiftPMImport/emptyxcode/handleUserActivity(userActivity: kotlin/Any): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.swiftPMImport/emptyxcode/initiateOnDeviceConversionMeasurementWithEmailAddress(emailAddress: kotlin/String): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.swiftPMImport/emptyxcode/initiateOnDeviceConversionMeasurementWithHashedEmailAddress(hashedEmailAddress: platform/Foundation/NSData): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.swiftPMImport/emptyxcode/initiateOnDeviceConversionMeasurementWithHashedPhoneNumber(hashedPhoneNumber: platform/Foundation/NSData): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.swiftPMImport/emptyxcode/initiateOnDeviceConversionMeasurementWithPhoneNumber(phoneNumber: kotlin/String): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.swiftPMImport/emptyxcode/setConsent(consentSettings: kotlin/collections/Map<kotlin/Any?, *>): kotlin/Unit
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
            "provideAPIKey" to "public open expect fun swiftPMImport/emptyxcode/GMSServicesMeta.provideAPIKey(APIKey: kotlin/String): kotlin/Boolean",
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
            "startWithConfigureOptions" to "public open expect fun swiftPMImport/emptyxcode/SentrySDKMeta.startWithConfigureOptions(configureOptions: kotlin/Function1<swiftPMImport/emptyxcode/SentryOptions?, kotlin/Unit>): kotlin/Unit",
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
        public open expect fun swiftPMImport/emptyxcode/RCPurchases.purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withCompletion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/RCPurchases.purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withPromotionalOffer: swiftPMImport/emptyxcode/RCPromotionalOffer, completion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
        public abstract expect fun swiftPMImport/emptyxcode/RCPurchasesTypeProtocol.purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withCompletion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
        public abstract expect fun swiftPMImport/emptyxcode/RCPurchasesTypeProtocol.purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withPromotionalOffer: swiftPMImport/emptyxcode/RCPromotionalOffer, completion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/RCPurchases.swiftPMImport/emptyxcode/purchaseProduct(product: objcnames/classes/SKProduct, withCompletionBlock: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/RCPurchases.swiftPMImport/emptyxcode/purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withCompletion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/RCPurchases.swiftPMImport/emptyxcode/purchaseProduct(product: objcnames/classes/SKProduct, withDiscount: objcnames/classes/SKPaymentDiscount, completionBlock: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/RCPurchases.swiftPMImport/emptyxcode/purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withPromotionalOffer: swiftPMImport/emptyxcode/RCPromotionalOffer, completion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
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
        isStatic = isStatic,
        // FIXME: KT-87246 - remove this parameter after Xcode 27 is stable
        checkForObjCRuntimeWarnings = Xcode.findCurrent().version.major < 27
    ) { _ ->
        swiftPackage(
            url = url("https://github.com/RevenueCat/purchases-ios-spm.git"),
            version = exact("5.79.0"),
            products = listOf(product("RevenueCat")),
        )
    }

    @DisplayName("direct dependency on AWS")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on AWS`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "defaultEC2" to "public open expect fun swiftPMImport/emptyxcode/AWSEC2Meta.defaultEC2(): swiftPMImport/emptyxcode/AWSEC2",
            "AWSEC2DescribeInstancesRequest" to """
        public open expect fun swiftPMImport/emptyxcode/AWSEC2.describeInstances(request: swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest): swiftPMImport/emptyxcode/AWSTask
        public open expect fun swiftPMImport/emptyxcode/AWSEC2.describeInstances(request: swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest, completionHandler: kotlin/Function2<swiftPMImport/emptyxcode/AWSEC2DescribeInstancesResult?, platform/Foundation/NSError?, kotlin/Unit>?): kotlin/Unit
        public open expect class swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest : swiftPMImport/emptyxcode/AWSRequest
        public /* secondary */ constructor swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.<init>()
        public /* secondary */ constructor swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.<init>(coder: platform/Foundation/NSCoder)
        public /* secondary */ constructor swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.<init>(dictionary: kotlin/collections/Map<kotlin/Any?, *>?, error: kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCObjectVar<platform/Foundation/NSError?>>?)
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.dryRun(): platform/Foundation/NSNumber?
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.filters(): kotlin/collections/List<*>?
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.init(): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.initWithCoder(coder: platform/Foundation/NSCoder): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.initWithDictionary(dictionaryValue: kotlin/collections/Map<kotlin/Any?, *>?, error: kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCObjectVar<platform/Foundation/NSError?>>?): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.instanceIds(): kotlin/collections/List<*>?
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.maxResults(): platform/Foundation/NSNumber?
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.nextToken(): kotlin/String?
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.setDryRun(dryRun: platform/Foundation/NSNumber?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.setFilters(filters: kotlin/collections/List<*>?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.setInstanceIds(instanceIds: kotlin/collections/List<*>?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.setMaxResults(maxResults: platform/Foundation/NSNumber?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.setNextToken(nextToken: kotlin/String?): kotlin/Unit
        public final expect var swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.dryRun: platform/Foundation/NSNumber?
        public final /* getter */ swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.dryRun.<get-dryRun>
        public final /* setter */ swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.dryRun.<set-dryRun>
        public final expect var swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.filters: kotlin/collections/List<*>?
        public final /* getter */ swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.filters.<get-filters>
        public final /* setter */ swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.filters.<set-filters>
        public final expect var swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.instanceIds: kotlin/collections/List<*>?
        public final /* getter */ swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.instanceIds.<get-instanceIds>
        public final /* setter */ swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.instanceIds.<set-instanceIds>
        public final expect var swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.maxResults: platform/Foundation/NSNumber?
        public final /* getter */ swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.maxResults.<get-maxResults>
        public final /* setter */ swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.maxResults.<set-maxResults>
        public final expect var swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.nextToken: kotlin/String?
        public final /* getter */ swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.nextToken.<get-nextToken>
        public final /* setter */ swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.nextToken.<set-nextToken>
        public final expect companion object swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.Companion : swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequestMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest>
        public open expect class swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequestMeta : swiftPMImport/emptyxcode/AWSRequestMeta
        protected /* secondary */ constructor swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequestMeta.<init>()
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequestMeta.alloc(): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequestMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequestMeta.modelWithDictionary(dictionaryValue: kotlin/collections/Map<kotlin/Any?, *>?, error: kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCObjectVar<platform/Foundation/NSError?>>?): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequestMeta.new(): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
    """.trimIndent(),
            "AWSEC2ImageAttributeName" to """
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeImageAttributeRequest.attribute(): swiftPMImport/emptyxcode/AWSEC2ImageAttributeName
        public open expect fun swiftPMImport/emptyxcode/AWSEC2DescribeImageAttributeRequest.setAttribute(attribute: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName): kotlin/Unit
        public final expect var swiftPMImport/emptyxcode/AWSEC2DescribeImageAttributeRequest.attribute: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName
        public final expect enum class swiftPMImport/emptyxcode/AWSEC2ImageAttributeName : kotlin/Enum<swiftPMImport/emptyxcode/AWSEC2ImageAttributeName>, kotlinx/cinterop/CEnum
        public open expect val swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.value: kotlin/Long
        public open /* getter */ swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.value.<get-value>
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameSriovNetSupport
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLastLaunchedTime
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUnknown
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBootMode
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLaunchPermission
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBlockDeviceMapping
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDeregistrationProtection
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameImdsSupport
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUefiData
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameProductCodes
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameTpmSupport
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameRAMDisk
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameKernel
        enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDescription
        public final expect companion object swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Companion : kotlin/Any
        public final expect fun swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Companion.byValue(value: kotlin/Long): swiftPMImport/emptyxcode/AWSEC2ImageAttributeName
        public final expect class swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Var : kotlinx/cinterop/CEnumVar
        public constructor swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Var.<init>(rawPtr: kotlin/native/internal/NativePtr)
        public final expect var swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Var.value: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName
        public final /* getter */ swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Var.value.<get-value>
        public final /* setter */ swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Var.value.<set-value>
        public final expect companion object swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Var.Companion : kotlinx/cinterop/CPrimitiveVar.Type
    """.trimIndent(),
            "AWSS3TransferUtilityDownloadTask" to """
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtility.downloadDataForKey(key: kotlin/String, expression: swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?): swiftPMImport/emptyxcode/AWSTask
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtility.downloadDataFromBucket(bucket: kotlin/String, key: kotlin/String, expression: swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?): swiftPMImport/emptyxcode/AWSTask
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtility.downloadToURL(fileURL: platform/Foundation/NSURL, key: kotlin/String, expression: swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?): swiftPMImport/emptyxcode/AWSTask
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtility.downloadToURL(fileURL: platform/Foundation/NSURL, bucket: kotlin/String, key: kotlin/String, expression: swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?): swiftPMImport/emptyxcode/AWSTask
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtility.enumerateToAssignBlocksForUploadTask(uploadBlocksAssigner: kotlin/Function3<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>?>>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>?>>?, kotlin/Unit>?, downloadTask: kotlin/Function3<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>?>>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?>>?, kotlin/Unit>?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtility.enumerateToAssignBlocksForUploadTask(uploadBlocksAssigner: kotlin/Function3<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>?>>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>?>>?, kotlin/Unit>, multiPartUploadBlocksAssigner: kotlin/Function3<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSProgress?, kotlin/Unit>?>>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSError?, kotlin/Unit>?>>?, kotlin/Unit>, downloadBlocksAssigner: kotlin/Function3<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>?>>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?>>?, kotlin/Unit>): kotlin/Unit
        public /* secondary */ constructor swiftPMImport/emptyxcode/AWSS3TransferUtilityBlocks.<init>(uploadProgress: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>?, multiPartUploadProgress: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSProgress?, kotlin/Unit>?, downloadProgress: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>?, uploadCompleted: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>?, multiPartUploadCompleted: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSError?, kotlin/Unit>?, downloadCompleted: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?)
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtilityBlocks.downloadCompletedBlock(): kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtilityBlocks.initWithUploadProgress(uploadProgressBlock: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>?, multiPartUploadProgress: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSProgress?, kotlin/Unit>?, downloadProgress: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>?, uploadCompleted: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>?, multiPartUploadCompleted: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSError?, kotlin/Unit>?, downloadCompleted: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?): swiftPMImport/emptyxcode/AWSS3TransferUtilityBlocks
        public final expect val swiftPMImport/emptyxcode/AWSS3TransferUtilityBlocks.downloadCompletedBlock: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?
        public open expect class swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask : swiftPMImport/emptyxcode/AWSS3TransferUtilityTask
        public /* secondary */ constructor swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask.<init>()
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask.init(): swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask.setCompletionHandler(completionHandler: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask.setProgressBlock(progressBlock: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>?): kotlin/Unit
        public final expect companion object swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask.Companion : swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTaskMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask>
        public open expect class swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTaskMeta : swiftPMImport/emptyxcode/AWSS3TransferUtilityTaskMeta
        protected /* secondary */ constructor swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTaskMeta.<init>()
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTaskMeta.alloc(): swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTaskMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?
        public open expect fun swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTaskMeta.new(): swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?
        public typealias swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock = kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?
        public typealias swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlockVar = kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?>
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
        public open expect class swiftPMImport/emptyxcode/MapView : platform/UIKit/UIView
        public /* secondary */ constructor swiftPMImport/emptyxcode/MapView.<init>()
        public /* secondary */ constructor swiftPMImport/emptyxcode/MapView.<init>(frame: kotlinx/cinterop/CValue<platform/CoreGraphics/CGRect>)
        public /* secondary */ constructor swiftPMImport/emptyxcode/MapView.<init>(coder: platform/Foundation/NSCoder)
        public open expect fun swiftPMImport/emptyxcode/MapView.awakeFromNib(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/MapView.init(): swiftPMImport/emptyxcode/MapView
        public open expect fun swiftPMImport/emptyxcode/MapView.initWithCoder(coder: platform/Foundation/NSCoder): swiftPMImport/emptyxcode/MapView?
        public open expect fun swiftPMImport/emptyxcode/MapView.initWithFrame(frame: kotlinx/cinterop/CValue<platform/CoreGraphics/CGRect>): swiftPMImport/emptyxcode/MapView
        public final expect companion object swiftPMImport/emptyxcode/MapView.Companion : swiftPMImport/emptyxcode/MapViewMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/MapView>
        public open expect class swiftPMImport/emptyxcode/MapViewMeta : platform/UIKit/UIViewMeta
        protected /* secondary */ constructor swiftPMImport/emptyxcode/MapViewMeta.<init>()
        public open expect fun swiftPMImport/emptyxcode/MapViewMeta.alloc(): swiftPMImport/emptyxcode/MapView?
        public open expect fun swiftPMImport/emptyxcode/MapViewMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/MapView?
        public open expect fun swiftPMImport/emptyxcode/MapViewMeta.appearance(): swiftPMImport/emptyxcode/MapView
        public open expect fun swiftPMImport/emptyxcode/MapViewMeta.appearanceForTraitCollection(trait: platform/UIKit/UITraitCollection): swiftPMImport/emptyxcode/MapView
        public open expect fun swiftPMImport/emptyxcode/MapViewMeta.appearanceForTraitCollection(trait: platform/UIKit/UITraitCollection, whenContainedInInstancesOfClasses: kotlin/collections/List<*>): swiftPMImport/emptyxcode/MapView
        public open expect fun swiftPMImport/emptyxcode/MapViewMeta.appearanceForTraitCollection(trait: platform/UIKit/UITraitCollection, whenContainedIn: platform/UIKit/UIAppearanceContainerProtocol?, vararg args: kotlin/Any?): swiftPMImport/emptyxcode/MapView
        public open expect fun swiftPMImport/emptyxcode/MapViewMeta.appearanceWhenContainedIn(ContainerClass: platform/UIKit/UIAppearanceContainerProtocol?, vararg args: kotlin/Any?): swiftPMImport/emptyxcode/MapView
        public open expect fun swiftPMImport/emptyxcode/MapViewMeta.appearanceWhenContainedInInstancesOfClasses(containerTypes: kotlin/collections/List<*>): swiftPMImport/emptyxcode/MapView
        public open expect fun swiftPMImport/emptyxcode/MapViewMeta.new(): swiftPMImport/emptyxcode/MapView?
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
        public final expect fun swiftPMImport/emptyxcode/hev_socks5_tunnel_main(config_path: kotlin/String?, tun_fd: kotlin/Int): kotlin/Int
        public final expect fun swiftPMImport/emptyxcode/hev_socks5_tunnel_main(config_path: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ByteVarOf<kotlin/Byte>>?, tun_fd: kotlin/Int): kotlin/Int
        public final expect fun swiftPMImport/emptyxcode/hev_socks5_tunnel_main_from_file(config_path: kotlin/String?, tun_fd: kotlin/Int): kotlin/Int
        public final expect fun swiftPMImport/emptyxcode/hev_socks5_tunnel_main_from_file(config_path: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ByteVarOf<kotlin/Byte>>?, tun_fd: kotlin/Int): kotlin/Int
        public final expect fun swiftPMImport/emptyxcode/hev_socks5_tunnel_main_from_str(config_str: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/UByteVarOf<kotlin/UByte>>?, config_len: kotlin/UInt, tun_fd: kotlin/Int): kotlin/Int
        public final expect fun swiftPMImport/emptyxcode/hev_socks5_tunnel_quit(): kotlin/Unit
        public final expect fun swiftPMImport/emptyxcode/hev_socks5_tunnel_stats(tx_packets: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong>>?, tx_bytes: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong>>?, rx_packets: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong>>?, rx_bytes: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong>>?): kotlin/Unit
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
        public open expect class swiftPMImport/emptyxcode/DDDatadog : platform/darwin/NSObject
        public /* secondary */ constructor swiftPMImport/emptyxcode/DDDatadog.<init>()
        public open expect fun swiftPMImport/emptyxcode/DDDatadog.init(): swiftPMImport/emptyxcode/DDDatadog
        public final expect companion object swiftPMImport/emptyxcode/DDDatadog.Companion : swiftPMImport/emptyxcode/DDDatadogMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/DDDatadog>
        public open expect class swiftPMImport/emptyxcode/DDDatadogMeta : platform/darwin/NSObjectMeta
        protected /* secondary */ constructor swiftPMImport/emptyxcode/DDDatadogMeta.<init>()
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.addAccountExtraInfo(extraInfo: kotlin/collections/Map<kotlin/Any?, *>): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.addUserExtraInfo(extraInfo: kotlin/collections/Map<kotlin/Any?, *>): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.alloc(): swiftPMImport/emptyxcode/DDDatadog?
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/DDDatadog?
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.clearAccountInfo(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.clearAllData(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.clearUserInfo(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.initializeWithConfiguration(configuration: swiftPMImport/emptyxcode/DDConfiguration, trackingConsent: swiftPMImport/emptyxcode/DDTrackingConsent): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.isInitialized(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.new(): swiftPMImport/emptyxcode/DDDatadog?
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.setAccountInfoWithAccountId(accountId: kotlin/String, name: kotlin/String?, extraInfo: kotlin/collections/Map<kotlin/Any?, *>): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.setTrackingConsentWithConsent(consent: swiftPMImport/emptyxcode/DDTrackingConsent): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.setUserInfoWithUserId(userId: kotlin/String, name: kotlin/String?, email: kotlin/String?, extraInfo: kotlin/collections/Map<kotlin/Any?, *>): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.setVerbosityLevel(verbosityLevel: kotlin/Long): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.stopInstance(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDDatadogMeta.verbosityLevel(): kotlin/Long
    """.trimIndent(),
            "DDLogs" to """
        public open expect class swiftPMImport/emptyxcode/DDLogs : platform/darwin/NSObject
        public /* secondary */ constructor swiftPMImport/emptyxcode/DDLogs.<init>()
        public open expect fun swiftPMImport/emptyxcode/DDLogs.init(): swiftPMImport/emptyxcode/DDLogs
        public final expect companion object swiftPMImport/emptyxcode/DDLogs.Companion : swiftPMImport/emptyxcode/DDLogsMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/DDLogs>
        public open expect class swiftPMImport/emptyxcode/DDLogsConfiguration : platform/darwin/NSObject
        public /* secondary */ constructor swiftPMImport/emptyxcode/DDLogsConfiguration.<init>()
        public /* secondary */ constructor swiftPMImport/emptyxcode/DDLogsConfiguration.<init>(customEndpoint: platform/Foundation/NSURL?)
        public open expect fun swiftPMImport/emptyxcode/DDLogsConfiguration.customEndpoint(): platform/Foundation/NSURL?
        public open expect fun swiftPMImport/emptyxcode/DDLogsConfiguration.init(): swiftPMImport/emptyxcode/DDLogsConfiguration?
        public open expect fun swiftPMImport/emptyxcode/DDLogsConfiguration.initWithCustomEndpoint(customEndpoint: platform/Foundation/NSURL?): swiftPMImport/emptyxcode/DDLogsConfiguration
        public open expect fun swiftPMImport/emptyxcode/DDLogsConfiguration.setCustomEndpoint(customEndpoint: platform/Foundation/NSURL?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDLogsConfiguration.setEventMapper(mapper: kotlin/Function1<swiftPMImport/emptyxcode/DDLogEvent?, swiftPMImport/emptyxcode/DDLogEvent?>): kotlin/Unit
        public final expect var swiftPMImport/emptyxcode/DDLogsConfiguration.customEndpoint: platform/Foundation/NSURL?
        public final /* getter */ swiftPMImport/emptyxcode/DDLogsConfiguration.customEndpoint.<get-customEndpoint>
        public final /* setter */ swiftPMImport/emptyxcode/DDLogsConfiguration.customEndpoint.<set-customEndpoint>
        public final expect companion object swiftPMImport/emptyxcode/DDLogsConfiguration.Companion : swiftPMImport/emptyxcode/DDLogsConfigurationMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/DDLogsConfiguration>
        public open expect class swiftPMImport/emptyxcode/DDLogsConfigurationMeta : platform/darwin/NSObjectMeta
        protected /* secondary */ constructor swiftPMImport/emptyxcode/DDLogsConfigurationMeta.<init>()
        public open expect fun swiftPMImport/emptyxcode/DDLogsConfigurationMeta.alloc(): swiftPMImport/emptyxcode/DDLogsConfiguration?
        public open expect fun swiftPMImport/emptyxcode/DDLogsConfigurationMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/DDLogsConfiguration?
        public open expect fun swiftPMImport/emptyxcode/DDLogsConfigurationMeta.new(): swiftPMImport/emptyxcode/DDLogsConfiguration?
        public open expect class swiftPMImport/emptyxcode/DDLogsMeta : platform/darwin/NSObjectMeta
        protected /* secondary */ constructor swiftPMImport/emptyxcode/DDLogsMeta.<init>()
        public open expect fun swiftPMImport/emptyxcode/DDLogsMeta.addAttributeForKey(key: kotlin/String, value: kotlin/Any): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDLogsMeta.alloc(): swiftPMImport/emptyxcode/DDLogs?
        public open expect fun swiftPMImport/emptyxcode/DDLogsMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/DDLogs?
        public open expect fun swiftPMImport/emptyxcode/DDLogsMeta.enableWith(configuration: swiftPMImport/emptyxcode/DDLogsConfiguration): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/DDLogsMeta.new(): swiftPMImport/emptyxcode/DDLogs?
        public open expect fun swiftPMImport/emptyxcode/DDLogsMeta.removeAttributeForKey(key: kotlin/String): kotlin/Unit
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
        public open expect fun swiftPMImport/emptyxcode/AdjustBridge.loadWKWebViewBridge(wkWebView: platform/WebKit/WKWebView): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/AdjustBridge.setWkWebView(wkWebView: platform/WebKit/WKWebView): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/AdjustBridge.wkWebView(): platform/WebKit/WKWebView
        public final expect var swiftPMImport/emptyxcode/AdjustBridge.wkWebView: platform/WebKit/WKWebView
    """.trimIndent(),
            "ADJConfig" to """
        public open expect class swiftPMImport/emptyxcode/ADJConfig : platform/darwin/NSObject, platform/Foundation/NSCopyingProtocol
        public /* secondary */ constructor swiftPMImport/emptyxcode/ADJConfig.<init>()
        public /* secondary */ constructor swiftPMImport/emptyxcode/ADJConfig.<init>(appToken: kotlin/String, environment: kotlin/String)
        public /* secondary */ constructor swiftPMImport/emptyxcode/ADJConfig.<init>(appToken: kotlin/String, environment: kotlin/String, suppressLogLevel: kotlin/Boolean)
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.appToken(): kotlin/String
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.attConsentWaitingInterval(): kotlin/ULong
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.copyWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): kotlin/Any
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.defaultTracker(): kotlin/String?
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.delegate(): platform/darwin/NSObject?
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.disableAdServices(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.disableAppTrackingTransparencyUsage(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.disableIdfaReading(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.disableIdfvReading(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.disableSkanAttribution(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.enableCoppaCompliance(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.enableCostDataInAttribution(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.enableDeviceIdsReadingOnce(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.enableFirstSessionDelay(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.enableLinkMe(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.enableSendingInBackground(): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.environment(): kotlin/String
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.eventDeduplicationIdsMaxSize(): kotlin/Long
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.externalDeviceId(): kotlin/String?
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.init(): swiftPMImport/emptyxcode/ADJConfig?
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.initWithAppToken(appToken: kotlin/String, environment: kotlin/String): swiftPMImport/emptyxcode/ADJConfig?
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.initWithAppToken(appToken: kotlin/String, environment: kotlin/String, suppressLogLevel: kotlin/Boolean): swiftPMImport/emptyxcode/ADJConfig?
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isAdServicesEnabled(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isAppTrackingTransparencyUsageEnabled(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isCoppaComplianceEnabled(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isCostDataInAttributionEnabled(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isDataResidency(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isDeviceIdsReadingOnceEnabled(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isFirstSessionDelayEnabled(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isIdfaReadingEnabled(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isIdfvReadingEnabled(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isLinkMeEnabled(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isSendingInBackgroundEnabled(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isSkanAttributionEnabled(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.isValid(): kotlin/Boolean
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.logLevel(): kotlin/ULong
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.sdkPrefix(): kotlin/String?
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.setAttConsentWaitingInterval(attConsentWaitingInterval: kotlin/ULong): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.setDefaultTracker(defaultTracker: kotlin/String?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.setDelegate(delegate: platform/darwin/NSObject?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.setEventDeduplicationIdsMaxSize(eventDeduplicationIdsMaxSize: kotlin/Long): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.setExternalDeviceId(externalDeviceId: kotlin/String?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.setIsCoppaComplianceEnabled(isCoppaComplianceEnabled: kotlin/Boolean): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.setLogLevel(logLevel: kotlin/ULong): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.setSdkPrefix(sdkPrefix: kotlin/String?): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.setStoreInfo(storeInfo: swiftPMImport/emptyxcode/ADJStoreInfo): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.setUrlStrategy(urlStrategyDomains: kotlin/collections/List<*>?, useSubdomains: kotlin/Boolean, isDataResidency: kotlin/Boolean): kotlin/Unit
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.storeInfo(): swiftPMImport/emptyxcode/ADJStoreInfo
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.urlStrategyDomains(): kotlin/collections/List<*>?
        public open expect fun swiftPMImport/emptyxcode/ADJConfig.useSubdomains(): kotlin/Boolean
        public final expect val swiftPMImport/emptyxcode/ADJConfig.appToken: kotlin/String
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.appToken.<get-appToken>
        public final expect var swiftPMImport/emptyxcode/ADJConfig.attConsentWaitingInterval: kotlin/ULong
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.attConsentWaitingInterval.<get-attConsentWaitingInterval>
        public final /* setter */ swiftPMImport/emptyxcode/ADJConfig.attConsentWaitingInterval.<set-attConsentWaitingInterval>
        public final expect var swiftPMImport/emptyxcode/ADJConfig.defaultTracker: kotlin/String?
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.defaultTracker.<get-defaultTracker>
        public final /* setter */ swiftPMImport/emptyxcode/ADJConfig.defaultTracker.<set-defaultTracker>
        public final expect var swiftPMImport/emptyxcode/ADJConfig.delegate: platform/darwin/NSObject?
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.delegate.<get-delegate>
        public final /* setter */ swiftPMImport/emptyxcode/ADJConfig.delegate.<set-delegate>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.environment: kotlin/String
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.environment.<get-environment>
        public final expect var swiftPMImport/emptyxcode/ADJConfig.eventDeduplicationIdsMaxSize: kotlin/Long
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.eventDeduplicationIdsMaxSize.<get-eventDeduplicationIdsMaxSize>
        public final /* setter */ swiftPMImport/emptyxcode/ADJConfig.eventDeduplicationIdsMaxSize.<set-eventDeduplicationIdsMaxSize>
        public final expect var swiftPMImport/emptyxcode/ADJConfig.externalDeviceId: kotlin/String?
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.externalDeviceId.<get-externalDeviceId>
        public final /* setter */ swiftPMImport/emptyxcode/ADJConfig.externalDeviceId.<set-externalDeviceId>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.isAdServicesEnabled: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isAdServicesEnabled.<get-isAdServicesEnabled>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.isAppTrackingTransparencyUsageEnabled: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isAppTrackingTransparencyUsageEnabled.<get-isAppTrackingTransparencyUsageEnabled>
        public final expect var swiftPMImport/emptyxcode/ADJConfig.isCoppaComplianceEnabled: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isCoppaComplianceEnabled.<get-isCoppaComplianceEnabled>
        public final /* setter */ swiftPMImport/emptyxcode/ADJConfig.isCoppaComplianceEnabled.<set-isCoppaComplianceEnabled>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.isCostDataInAttributionEnabled: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isCostDataInAttributionEnabled.<get-isCostDataInAttributionEnabled>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.isDataResidency: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isDataResidency.<get-isDataResidency>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.isDeviceIdsReadingOnceEnabled: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isDeviceIdsReadingOnceEnabled.<get-isDeviceIdsReadingOnceEnabled>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.isFirstSessionDelayEnabled: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isFirstSessionDelayEnabled.<get-isFirstSessionDelayEnabled>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.isIdfaReadingEnabled: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isIdfaReadingEnabled.<get-isIdfaReadingEnabled>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.isIdfvReadingEnabled: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isIdfvReadingEnabled.<get-isIdfvReadingEnabled>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.isLinkMeEnabled: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isLinkMeEnabled.<get-isLinkMeEnabled>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.isSendingInBackgroundEnabled: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isSendingInBackgroundEnabled.<get-isSendingInBackgroundEnabled>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.isSkanAttributionEnabled: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.isSkanAttributionEnabled.<get-isSkanAttributionEnabled>
        public final expect var swiftPMImport/emptyxcode/ADJConfig.logLevel: kotlin/ULong
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.logLevel.<get-logLevel>
        public final /* setter */ swiftPMImport/emptyxcode/ADJConfig.logLevel.<set-logLevel>
        public final expect var swiftPMImport/emptyxcode/ADJConfig.sdkPrefix: kotlin/String?
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.sdkPrefix.<get-sdkPrefix>
        public final /* setter */ swiftPMImport/emptyxcode/ADJConfig.sdkPrefix.<set-sdkPrefix>
        public final expect var swiftPMImport/emptyxcode/ADJConfig.storeInfo: swiftPMImport/emptyxcode/ADJStoreInfo
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.storeInfo.<get-storeInfo>
        public final /* setter */ swiftPMImport/emptyxcode/ADJConfig.storeInfo.<set-storeInfo>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.urlStrategyDomains: kotlin/collections/List<*>?
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.urlStrategyDomains.<get-urlStrategyDomains>
        public final expect val swiftPMImport/emptyxcode/ADJConfig.useSubdomains: kotlin/Boolean
        public final /* getter */ swiftPMImport/emptyxcode/ADJConfig.useSubdomains.<get-useSubdomains>
        public final expect companion object swiftPMImport/emptyxcode/ADJConfig.Companion : swiftPMImport/emptyxcode/ADJConfigMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/ADJConfig>
        public open expect class swiftPMImport/emptyxcode/ADJConfigMeta : platform/darwin/NSObjectMeta, platform/Foundation/NSCopyingProtocolMeta
        protected /* secondary */ constructor swiftPMImport/emptyxcode/ADJConfigMeta.<init>()
        public open expect fun swiftPMImport/emptyxcode/ADJConfigMeta.alloc(): swiftPMImport/emptyxcode/ADJConfig?
        public open expect fun swiftPMImport/emptyxcode/ADJConfigMeta.allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/ADJConfig?
        public open expect fun swiftPMImport/emptyxcode/ADJConfigMeta.new(): swiftPMImport/emptyxcode/ADJConfig?
        public open expect fun swiftPMImport/emptyxcode/AdjustMeta.initSdk(adjustConfig: swiftPMImport/emptyxcode/ADJConfig?): kotlin/Unit
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
        public open expect fun swiftPMImport/emptyxcode/OIDServiceConfiguration.initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL): swiftPMImport/emptyxcode/OIDServiceConfiguration
        public open expect fun swiftPMImport/emptyxcode/OIDServiceConfiguration.initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, issuer: platform/Foundation/NSURL?): swiftPMImport/emptyxcode/OIDServiceConfiguration
        public open expect fun swiftPMImport/emptyxcode/OIDServiceConfiguration.initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, registrationEndpoint: platform/Foundation/NSURL?): swiftPMImport/emptyxcode/OIDServiceConfiguration
        public open expect fun swiftPMImport/emptyxcode/OIDServiceConfiguration.initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, issuer: platform/Foundation/NSURL?, registrationEndpoint: platform/Foundation/NSURL?): swiftPMImport/emptyxcode/OIDServiceConfiguration
        public open expect fun swiftPMImport/emptyxcode/OIDServiceConfiguration.initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, issuer: platform/Foundation/NSURL?, registrationEndpoint: platform/Foundation/NSURL?, endSessionEndpoint: platform/Foundation/NSURL?): swiftPMImport/emptyxcode/OIDServiceConfiguration
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
        isStatic = isStatic,
        // FIXME: KT-87246 - remove this parameter after Xcode 27 is stable
        checkForObjCRuntimeWarnings = Xcode.findCurrent().version.major < 27
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
            "greeting" to "public open expect fun swiftPMImport/emptyxcode/LocalHelperMeta.greeting(): kotlin/String",
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
                    "greeting" to "public open expect fun swiftPMImport/emptyxcode/producer/LocalHelperMeta.greeting(): kotlin/String",
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
        // FIXME: Remove this parameter after stable Xcode 27 deploys
        checkForObjCRuntimeWarnings: Boolean = true,
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
            assertApplicationRunsAndObjCRuntimeDoesntEmitInStderr(
                appPath,
                checkForObjCRuntimeWarnings = checkForObjCRuntimeWarnings
            )
            if (expectedPackageManifest != null) {
                testPackageManifest(expectedPackageManifest)
            }
        }
    }

    private class SpmImportArgumentsProvider : GradleArgumentsProvider() {
        override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
            return super.provideArguments(parameters, context).flatMap { arguments ->
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

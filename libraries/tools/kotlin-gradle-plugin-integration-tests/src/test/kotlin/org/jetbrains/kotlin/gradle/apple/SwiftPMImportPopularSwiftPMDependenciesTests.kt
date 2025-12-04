/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.junit.jupiter.api.condition.OS
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftImportExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.dumpKlibMetadata
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.test.assertEquals
import org.jetbrains.kotlin.gradle.util.isTeamCityRun
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.collections.mapOf
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.readText
import kotlin.io.path.writeText


@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@NativeGradlePluginTests
class SwiftPMImportPopularSwiftPMDependenciesTests : KGPBaseTest() {

    @DisplayName("direct dependency on Firebase")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on Firebase`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "firestoreForApp" to """
@kotlinx/cinterop/ObjCMethod(encoding = "@24@0:8@16", selector = "firestoreForApp:", isStret = false)
public open expect fun firestoreForApp(app: swiftPMImport/emptyxcode/FIRApp): swiftPMImport/emptyxcode/FIRFirestore
@kotlinx/cinterop/ObjCMethod(encoding = "@32@0:8@16@24", selector = "firestoreForApp:database:", isStret = false)
public open expect fun firestoreForApp(app: swiftPMImport/emptyxcode/FIRApp, database: kotlin/String): swiftPMImport/emptyxcode/FIRFirestore
                """.trimIndent(),
            "FIRAnalytics" to """
// class name: swiftPMImport/emptyxcode/FIRAnalytics
// class name: swiftPMImport/emptyxcode/FIRAnalytics.Companion
// class name: swiftPMImport/emptyxcode/FIRAnalyticsMeta
public open expect class swiftPMImport/emptyxcode/FIRAnalytics : platform/darwin/NSObject {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "FIRAnalytics()"))
  public open expect fun init(): swiftPMImport/emptyxcode/FIRAnalytics?
public final expect companion object swiftPMImport/emptyxcode/FIRAnalytics.Companion : swiftPMImport/emptyxcode/FIRAnalyticsMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/FIRAnalytics> {
public open expect class swiftPMImport/emptyxcode/FIRAnalyticsMeta : platform/darwin/NSObjectMeta {
  public open expect fun alloc(): swiftPMImport/emptyxcode/FIRAnalytics?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/FIRAnalytics?
  public open expect fun new(): swiftPMImport/emptyxcode/FIRAnalytics?
  public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.handleEventsForBackgroundURLSession(identifier: kotlin/String, completionHandler: kotlin/Function0<kotlin/Unit>?): kotlin/Unit
  public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.handleOpenURL(url: platform/Foundation/NSURL): kotlin/Unit
  public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.handleUserActivity(userActivity: kotlin/Any): kotlin/Unit
  public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithEmailAddress(emailAddress: kotlin/String): kotlin/Unit
  public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithHashedEmailAddress(hashedEmailAddress: platform/Foundation/NSData): kotlin/Unit
  public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithHashedPhoneNumber(hashedPhoneNumber: platform/Foundation/NSData): kotlin/Unit
  public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithPhoneNumber(phoneNumber: kotlin/String): kotlin/Unit
  public final expect fun swiftPMImport/emptyxcode/FIRAnalyticsMeta.setConsent(consentSettings: kotlin/collections/Map<kotlin/Any?, *>): kotlin/Unit
                """.trimIndent()
        ),
        ktSnippet = """
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun createFirestore(): swiftPMImport.emptyxcode.FIRFirestore {
                return swiftPMImport.emptyxcode.FIRFirestore.Companion.firestore()
            }
        """.trimIndent(),
        swiftSnippet = """
            import SwiftUI
            import FirebaseCore
            import FirebaseFirestore
            import Shared

            @main
            struct iOSApp: App {
                init() {
                    let opts = FirebaseOptions(googleAppID: "1:1234567890:ios:abcdef123456", gcmSenderID: "1234567890")
                    opts.apiKey = "AIzaSyDrandomKeyGeneratedForDebug001234"
                    opts.projectID = "dummy"
                    FirebaseApp.configure(options: opts)

                    TempKt.createFirestore().collection("users").document("local_user")
                        .setData(["name": "John Doe", "isOffline": true])
                }

                var body: some Scene { WindowGroup { Text("OK") } }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) {
        `package`(
            url = url("https://github.com/firebase/firebase-ios-sdk.git"),
            version = exact("12.5.0"),
            products = listOf(product("FirebaseAnalytics"), product("FirebaseFirestore")),
        )
        `package`(
            url = url("https://github.com/apple/swift-protobuf.git"),
            version = exact("1.32.0"),
            products = listOf(),
        )
    }

    @DisplayName("direct dependency on GoogleMaps")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on GoogleMaps`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "provideAPIKey" to """
                @kotlinx/cinterop/ObjCMethod(encoding = "B24@0:8@16", selector = "provideAPIKey:", isStret = false)
                public open expect fun provideAPIKey(APIKey: kotlin/String): kotlin/Boolean
                """.trimIndent()
        ),
        swiftSnippet = """
            import SwiftUI
            import GoogleMaps
            import Shared

            @main
            struct iOSApp: App {
                init() { GMSServices.provideAPIKey("APIKEY") }
                
                var body: some Scene {
                    WindowGroup { MapView() }
                }
            }

            struct MapView: UIViewRepresentable {
                func makeUIView(context: Context) -> GMSMapView {
                    // .zero frame is used because SwiftUI handles layout resizing automatically
                    .map(withFrame: .zero, camera: TempKt.googleMapsCameraPosition())
                }
                
                func updateUIView(_ view: GMSMapView, context: Context) {}
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
    ) {
        iosDeploymentVersion.set("16.0")
        `package`(
            url = url("git@github.com:googlemaps/ios-maps-sdk.git"),
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
                @kotlinx/cinterop/ObjCMethod(encoding = "v24@0:8@?16", selector = "startWithConfigureOptions:", isStret = false)
                public open expect fun startWithConfigureOptions(configureOptions: kotlin/Function1<swiftPMImport/emptyxcode/SentryOptions?, kotlin/Unit>): kotlin/Unit
                """.trimIndent()
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
            import SwiftUI
            import Sentry
            import Shared

            @main
            struct iOSApp: App {
                var body: some Scene { WindowGroup {
                    let eventID = TempKt.sentryTest()
                    Text("Sentry Event ID: \(eventID)")
                } }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) {
        `package`(
            url = url("https://github.com/getsentry/sentry-cocoa.git"),
            version = exact("9.0.0-rc.1"), // use rc to get the fix: https://github.com/getsentry/sentry-cocoa/pull/6607
            products = listOf(product("Sentry")),
        )
    }

    @DisplayName("direct dependency on RevenueCat")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(SpmImportArgumentsProvider::class)
    fun `direct dependency on RevenueCat`(version: GradleVersion, isStatic: Boolean) = testSwiftPackageIntegration(
        version = version,
        expectedCinteropAPIs = mapOf(
            "purchaseProduct" to """@kotlinx/cinterop/ObjCMethod(encoding = "v32@0:8@16@?24", selector = "purchaseProduct:withCompletion:", isStret = false)
public open expect fun purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withCompletion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v40@0:8@16@24@?32", selector = "purchaseProduct:withPromotionalOffer:completion:", isStret = false)
public open expect fun purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withPromotionalOffer: swiftPMImport/emptyxcode/RCPromotionalOffer, completion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v32@0:8@16@?24", selector = "purchaseProduct:withCompletion:", isStret = false)
public abstract expect fun purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withCompletion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v40@0:8@16@24@?32", selector = "purchaseProduct:withPromotionalOffer:completion:", isStret = false)
public abstract expect fun purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withPromotionalOffer: swiftPMImport/emptyxcode/RCPromotionalOffer, completion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v32@0:8@16@?24", selector = "purchaseProduct:withCompletionBlock:", isStret = false)
public final expect fun swiftPMImport/emptyxcode/RCPurchases.purchaseProduct(product: objcnames/classes/SKProduct, withCompletionBlock: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v32@0:8@16@?24", selector = "purchaseProduct:withCompletion:", isStret = false)
public final expect fun swiftPMImport/emptyxcode/RCPurchases.purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withCompletion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v40@0:8@16@24@?32", selector = "purchaseProduct:withDiscount:completionBlock:", isStret = false)
public final expect fun swiftPMImport/emptyxcode/RCPurchases.purchaseProduct(product: objcnames/classes/SKProduct, withDiscount: objcnames/classes/SKPaymentDiscount, completionBlock: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v40@0:8@16@24@?32", selector = "purchaseProduct:withPromotionalOffer:completion:", isStret = false)
public final expect fun swiftPMImport/emptyxcode/RCPurchases.purchaseProduct(product: swiftPMImport/emptyxcode/RCStoreProduct, withPromotionalOffer: swiftPMImport/emptyxcode/RCPromotionalOffer, completion: kotlin/Function4<swiftPMImport/emptyxcode/RCStoreTransaction?, swiftPMImport/emptyxcode/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
                """.trimIndent()),
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
            import SwiftUI
            import RevenueCat
            import Shared

            @main
            struct iOSApp: App {
                var body: some Scene {
                    WindowGroup {
                        let ktUserId = TempKt.revcat()
                        let localUserId = Purchases.shared.appUserID
                        Text("Is same: \(ktUserId == localUserId)")
                    }
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) {
        `package`(
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
            "defaultEC2" to """@kotlinx/cinterop/ObjCMethod(encoding = "@16@0:8", selector = "defaultEC2", isStret = false)
public open expect fun defaultEC2(): swiftPMImport/emptyxcode/AWSEC2
                """.trimIndent(),
            "AWSEC2DescribeInstancesRequest" to """// class name: swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest
// class name: swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.Companion
// class name: swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequestMeta
  public open expect fun describeInstances(request: swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest): swiftPMImport/emptyxcode/AWSTask
  public open expect fun describeInstances(request: swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest, completionHandler: kotlin/Function2<swiftPMImport/emptyxcode/AWSEC2DescribeInstancesResult?, platform/Foundation/NSError?, kotlin/Unit>?): kotlin/Unit
public open expect class swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest : swiftPMImport/emptyxcode/AWSRequest {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "AWSEC2DescribeInstancesRequest()"))
  public open expect fun init(): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "AWSEC2DescribeInstancesRequest(coder)"))
  public open expect fun initWithCoder(coder: platform/Foundation/NSCoder): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "AWSEC2DescribeInstancesRequest(dictionaryValue, error)"))
  public open expect fun initWithDictionary(dictionaryValue: kotlin/collections/Map<kotlin/Any?, *>?, error: kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCObjectVar<platform/Foundation/NSError?>>?): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
public final expect companion object swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest.Companion : swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequestMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest> {
public open expect class swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequestMeta : swiftPMImport/emptyxcode/AWSRequestMeta {
  public open expect fun alloc(): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
  public open expect fun modelWithDictionary(dictionaryValue: kotlin/collections/Map<kotlin/Any?, *>?, error: kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCObjectVar<platform/Foundation/NSError?>>?): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
  public open expect fun new(): swiftPMImport/emptyxcode/AWSEC2DescribeInstancesRequest?
            """.trimIndent(),
            "AWSEC2ImageAttributeName" to """// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBlockDeviceMapping
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBootMode
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDeregistrationProtection
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDescription
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameImdsSupport
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameKernel
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLastLaunchedTime
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLaunchPermission
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameProductCodes
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameRAMDisk
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameSriovNetSupport
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameTpmSupport
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUefiData
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUnknown
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Companion
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Var
// class name: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Var.Companion
  public open expect fun attribute(): swiftPMImport/emptyxcode/AWSEC2ImageAttributeName
  public open expect fun setAttribute(attribute: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName): kotlin/Unit
  public final expect var attribute: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName
public final expect enum class swiftPMImport/emptyxcode/AWSEC2ImageAttributeName : kotlin/Enum<swiftPMImport/emptyxcode/AWSEC2ImageAttributeName>, kotlinx/cinterop/CEnum {
  AWSEC2ImageAttributeNameSriovNetSupport,
  AWSEC2ImageAttributeNameLastLaunchedTime,
  AWSEC2ImageAttributeNameUnknown,
  AWSEC2ImageAttributeNameBootMode,
  AWSEC2ImageAttributeNameLaunchPermission,
  AWSEC2ImageAttributeNameBlockDeviceMapping,
  AWSEC2ImageAttributeNameDeregistrationProtection,
  AWSEC2ImageAttributeNameImdsSupport,
  AWSEC2ImageAttributeNameUefiData,
  AWSEC2ImageAttributeNameProductCodes,
  AWSEC2ImageAttributeNameTpmSupport,
  AWSEC2ImageAttributeNameRAMDisk,
  AWSEC2ImageAttributeNameKernel,
  AWSEC2ImageAttributeNameDescription,
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBlockDeviceMapping : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBootMode : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDeregistrationProtection : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDescription : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameImdsSupport : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameKernel : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLastLaunchedTime : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLaunchPermission : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameProductCodes : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameRAMDisk : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameSriovNetSupport : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameTpmSupport : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUefiData : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUnknown : swiftPMImport/emptyxcode/AWSEC2ImageAttributeName {
public final expect companion object swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Companion : kotlin/Any {
  public final expect fun byValue(value: kotlin/Long /* = platform/darwin/NSInteger^ */): swiftPMImport/emptyxcode/AWSEC2ImageAttributeName
public final expect class swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Var : kotlinx/cinterop/CEnumVar {
  public final expect var value: swiftPMImport/emptyxcode/AWSEC2ImageAttributeName
public final expect companion object swiftPMImport/emptyxcode/AWSEC2ImageAttributeName.Var.Companion : kotlinx/cinterop/CPrimitiveVar.Type {
            """.trimIndent(),
            "AWSS3TransferUtilityDownloadTask" to """// class name: swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask
// class name: swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask.Companion
// class name: swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTaskMeta
  public open expect fun downloadDataForKey(key: kotlin/String, expression: swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): swiftPMImport/emptyxcode/AWSTask
  public open expect fun downloadDataFromBucket(bucket: kotlin/String, key: kotlin/String, expression: swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): swiftPMImport/emptyxcode/AWSTask
  public open expect fun downloadToURL(fileURL: platform/Foundation/NSURL, key: kotlin/String, expression: swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): swiftPMImport/emptyxcode/AWSTask
  public open expect fun downloadToURL(fileURL: platform/Foundation/NSURL, bucket: kotlin/String, key: kotlin/String, expression: swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): swiftPMImport/emptyxcode/AWSTask
  public open expect fun enumerateToAssignBlocksForUploadTask(uploadBlocksAssigner: kotlin/Function3<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlock^? */> /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlockVar^ */>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadCompletionHandlerBlock^? */> /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadCompletionHandlerBlockVar^ */>?, kotlin/Unit>?, downloadTask: kotlin/Function3<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlock^? */> /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlockVar^ */>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */> /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlockVar^ */>?, kotlin/Unit>?): kotlin/Unit
  public open expect fun enumerateToAssignBlocksForUploadTask(uploadBlocksAssigner: kotlin/Function3<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlock^? */> /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlockVar^ */>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadCompletionHandlerBlock^? */> /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadCompletionHandlerBlockVar^ */>?, kotlin/Unit>, multiPartUploadBlocksAssigner: kotlin/Function3<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartProgressBlock^? */> /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartProgressBlockVar^ */>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadCompletionHandlerBlock^? */> /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadCompletionHandlerBlockVar^ */>?, kotlin/Unit>, downloadBlocksAssigner: kotlin/Function3<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlock^? */> /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlockVar^ */>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */> /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlockVar^ */>?, kotlin/Unit>): kotlin/Unit
  public /* secondary */ constructor(uploadProgress: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlock^? */, multiPartUploadProgress: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartProgressBlock^? */, downloadProgress: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlock^? */, uploadCompleted: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadCompletionHandlerBlock^? */, multiPartUploadCompleted: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadCompletionHandlerBlock^? */, downloadCompleted: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */)
  public open expect fun downloadCompletedBlock(): kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */
  public open expect fun initWithUploadProgress(uploadProgressBlock: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlock^? */, multiPartUploadProgress: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartProgressBlock^? */, downloadProgress: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityProgressBlock^? */, uploadCompleted: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityUploadCompletionHandlerBlock^? */, multiPartUploadCompleted: kotlin/Function2<swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityMultiPartUploadCompletionHandlerBlock^? */, downloadCompleted: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): swiftPMImport/emptyxcode/AWSS3TransferUtilityBlocks
  public final expect val downloadCompletedBlock: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */
public open expect class swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask : swiftPMImport/emptyxcode/AWSS3TransferUtilityTask {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "AWSS3TransferUtilityDownloadTask()"))
  public open expect fun init(): swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?
  public open expect fun setCompletionHandler(completionHandler: kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): kotlin/Unit
public final expect companion object swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask.Companion : swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTaskMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask> {
public open expect class swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTaskMeta : swiftPMImport/emptyxcode/AWSS3TransferUtilityTaskMeta {
  public open expect fun alloc(): swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?
  public open expect fun new(): swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?
  public typealias AWSS3TransferUtilityDownloadCompletionHandlerBlock = kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? */
  public typealias AWSS3TransferUtilityDownloadCompletionHandlerBlockVar = kotlinx/cinterop/ObjCBlockVar^<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^?> /* = kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?> /* = kotlinx/cinterop/ObjCBlockVar^<kotlin/Function4<swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/emptyxcode/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */> */ */
  """.trimIndent()
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
            import SwiftUI
            import AWSCore
            import Shared

            @main
            struct iOSApp: App {
                var body: some Scene {
                    WindowGroup {
                        let result = TempKt.aws(regionType: EPMIAWSRegionType.awsregioneunorth1,
                                                 credentialsProvider: AWSStaticCredentialsProvider(accessKey: "accessKey", secretKey: "secretKey"))
                        Text("Is Match: \(result)")
                    }
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) {
        `package`(
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
            "MapView" to """// class name: swiftPMImport/emptyxcode/MapView
// class name: swiftPMImport/emptyxcode/MapView.Companion
// class name: swiftPMImport/emptyxcode/MapViewMeta
public open expect class swiftPMImport/emptyxcode/MapView : platform/UIKit/UIView {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "MapView()"))
  public open expect fun init(): swiftPMImport/emptyxcode/MapView
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "MapView(coder)"))
  public open expect fun initWithCoder(coder: platform/Foundation/NSCoder): swiftPMImport/emptyxcode/MapView?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "MapView(frame)"))
  public open expect fun initWithFrame(frame: kotlinx/cinterop/CValue<platform/CoreGraphics/CGRect>): swiftPMImport/emptyxcode/MapView
public final expect companion object swiftPMImport/emptyxcode/MapView.Companion : swiftPMImport/emptyxcode/MapViewMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/MapView> {
public open expect class swiftPMImport/emptyxcode/MapViewMeta : platform/UIKit/UIViewMeta {
  public open expect fun alloc(): swiftPMImport/emptyxcode/MapView?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/MapView?
  public open expect fun appearance(): swiftPMImport/emptyxcode/MapView
  public open expect fun appearanceForTraitCollection(trait: platform/UIKit/UITraitCollection): swiftPMImport/emptyxcode/MapView
  public open expect fun appearanceForTraitCollection(trait: platform/UIKit/UITraitCollection, whenContainedInInstancesOfClasses: kotlin/collections/List<*>): swiftPMImport/emptyxcode/MapView
  public open expect fun appearanceForTraitCollection(trait: platform/UIKit/UITraitCollection, whenContainedIn: platform/UIKit/UIAppearanceContainerProtocol?, vararg args: kotlin/Any? /* kotlin/Array<kotlin/Any?> */): swiftPMImport/emptyxcode/MapView
  public open expect fun appearanceWhenContainedIn(ContainerClass: platform/UIKit/UIAppearanceContainerProtocol?, vararg args: kotlin/Any? /* kotlin/Array<kotlin/Any?> */): swiftPMImport/emptyxcode/MapView
  public open expect fun appearanceWhenContainedInInstancesOfClasses(containerTypes: kotlin/collections/List<*>): swiftPMImport/emptyxcode/MapView
  public open expect fun new(): swiftPMImport/emptyxcode/MapView?
  """.trimIndent()
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
            import SwiftUI
            import MapboxMaps
            import Shared

            @main
            struct iOSApp: App {
                var body: some Scene {
                    WindowGroup {
                        let center = CLLocationCoordinate2D(latitude: 39.5, longitude: -98.0)
                        Map(initialViewport: .camera(center: center, zoom: CGFloat(TempKt.mapboxZoom()), bearing: 0, pitch: 0))
                                    .ignoresSafeArea()
                    }
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) {
        `package`(
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
            "socks5_tunnel" to """public final expect fun hev_socks5_tunnel_main(config_path: kotlin/String?, tun_fd: kotlin/Int): kotlin/Int
public final expect fun hev_socks5_tunnel_main_from_file(config_path: kotlin/String?, tun_fd: kotlin/Int): kotlin/Int
public final expect fun hev_socks5_tunnel_main_from_str(config_str: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/UByteVarOf<kotlin/UByte> /* = kotlinx/cinterop/UByteVar^ */>?, config_len: kotlin/UInt, tun_fd: kotlin/Int): kotlin/Int
public final expect fun hev_socks5_tunnel_quit(): kotlin/Unit
public final expect fun hev_socks5_tunnel_stats(tx_packets: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong /* = platform/posix/size_t^ */> /* = platform/posix/size_tVar^ */>?, tx_bytes: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong /* = platform/posix/size_t^ */> /* = platform/posix/size_tVar^ */>?, rx_packets: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong /* = platform/posix/size_t^ */> /* = platform/posix/size_tVar^ */>?, rx_bytes: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong /* = platform/posix/size_t^ */> /* = platform/posix/size_tVar^ */>?): kotlin/Unit
  """.trimIndent()
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
            import SwiftUI
            import Shared
            import Tun2SocksKit

            @main
            struct iOSApp: App {
                var body: some Scene {
                    WindowGroup {
                        let connectionResult = TempKt.tunnel()
                        Text("Result: \(connectionResult)")
                        
                        let packets = Socks5Tunnel.stats.up.packets
                        Text("Packets Stats: \(packets)")
                    }
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) {
        `package`(
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
            "Datadog" to """// class name: swiftPMImport/emptyxcode/DDDatadog
// class name: swiftPMImport/emptyxcode/DDDatadog.Companion
// class name: swiftPMImport/emptyxcode/DDDatadogMeta
public open expect class swiftPMImport/emptyxcode/DDDatadog : platform/darwin/NSObject {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "DDDatadog()"))
  public open expect fun init(): swiftPMImport/emptyxcode/DDDatadog
public final expect companion object swiftPMImport/emptyxcode/DDDatadog.Companion : swiftPMImport/emptyxcode/DDDatadogMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/DDDatadog> {
public open expect class swiftPMImport/emptyxcode/DDDatadogMeta : platform/darwin/NSObjectMeta {
  public open expect fun alloc(): swiftPMImport/emptyxcode/DDDatadog?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/DDDatadog?
  public open expect fun new(): swiftPMImport/emptyxcode/DDDatadog?
  """.trimIndent(),
            "DDLogs" to """// class name: swiftPMImport/emptyxcode/DDLogs
// class name: swiftPMImport/emptyxcode/DDLogs.Companion
// class name: swiftPMImport/emptyxcode/DDLogsConfiguration
// class name: swiftPMImport/emptyxcode/DDLogsConfiguration.Companion
// class name: swiftPMImport/emptyxcode/DDLogsConfigurationMeta
// class name: swiftPMImport/emptyxcode/DDLogsMeta
public open expect class swiftPMImport/emptyxcode/DDLogs : platform/darwin/NSObject {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "DDLogs()"))
  public open expect fun init(): swiftPMImport/emptyxcode/DDLogs
public final expect companion object swiftPMImport/emptyxcode/DDLogs.Companion : swiftPMImport/emptyxcode/DDLogsMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/DDLogs> {
public open expect class swiftPMImport/emptyxcode/DDLogsConfiguration : platform/darwin/NSObject {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "DDLogsConfiguration()"))
  public open expect fun init(): swiftPMImport/emptyxcode/DDLogsConfiguration?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "DDLogsConfiguration(customEndpoint)"))
  public open expect fun initWithCustomEndpoint(customEndpoint: platform/Foundation/NSURL?): swiftPMImport/emptyxcode/DDLogsConfiguration
public final expect companion object swiftPMImport/emptyxcode/DDLogsConfiguration.Companion : swiftPMImport/emptyxcode/DDLogsConfigurationMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/DDLogsConfiguration> {
public open expect class swiftPMImport/emptyxcode/DDLogsConfigurationMeta : platform/darwin/NSObjectMeta {
  public open expect fun alloc(): swiftPMImport/emptyxcode/DDLogsConfiguration?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/DDLogsConfiguration?
  public open expect fun new(): swiftPMImport/emptyxcode/DDLogsConfiguration?
public open expect class swiftPMImport/emptyxcode/DDLogsMeta : platform/darwin/NSObjectMeta {
  public open expect fun alloc(): swiftPMImport/emptyxcode/DDLogs?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/DDLogs?
  public open expect fun enableWith(configuration: swiftPMImport/emptyxcode/DDLogsConfiguration): kotlin/Unit
  public open expect fun new(): swiftPMImport/emptyxcode/DDLogs?
            """.trimIndent()
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
            import SwiftUI
            import Shared
            import DatadogCore

            @main
            struct iOSApp: App {
                var body: some Scene {
                    WindowGroup {
                        let dd = TempKt.ddogInit()
                        let isInitialized = Datadog.isInitialized()
                        if(!isInitialized) { fatalError("DD should be initiated in K/N") }
                        Text("Is Initialized: \(isInitialized)")
                    }
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) {
        `package`(
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
            "WKWebView" to """@kotlinx/cinterop/ObjCMethod(encoding = "v24@0:8@16", selector = "loadWKWebViewBridge:", isStret = false)
public open expect fun loadWKWebViewBridge(wkWebView: platform/WebKit/WKWebView): kotlin/Unit
public open expect fun setWkWebView(wkWebView: platform/WebKit/WKWebView): kotlin/Unit
public open expect fun wkWebView(): platform/WebKit/WKWebView
public final expect var wkWebView: platform/WebKit/WKWebView
  """.trimIndent(),
            "ADJConfig" to """// class name: swiftPMImport/emptyxcode/ADJConfig
// class name: swiftPMImport/emptyxcode/ADJConfig.Companion
// class name: swiftPMImport/emptyxcode/ADJConfigMeta
public open expect class swiftPMImport/emptyxcode/ADJConfig : platform/darwin/NSObject, platform/Foundation/NSCopyingProtocol {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "ADJConfig()"))
  public open expect fun init(): swiftPMImport/emptyxcode/ADJConfig?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "ADJConfig(appToken, environment)"))
  public open expect fun initWithAppToken(appToken: kotlin/String, environment: kotlin/String): swiftPMImport/emptyxcode/ADJConfig?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "ADJConfig(appToken, environment, suppressLogLevel)"))
  public open expect fun initWithAppToken(appToken: kotlin/String, environment: kotlin/String, suppressLogLevel: kotlin/Boolean): swiftPMImport/emptyxcode/ADJConfig?
public final expect companion object swiftPMImport/emptyxcode/ADJConfig.Companion : swiftPMImport/emptyxcode/ADJConfigMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/emptyxcode/ADJConfig> {
public open expect class swiftPMImport/emptyxcode/ADJConfigMeta : platform/darwin/NSObjectMeta, platform/Foundation/NSCopyingProtocolMeta {
  public open expect fun alloc(): swiftPMImport/emptyxcode/ADJConfig?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/emptyxcode/ADJConfig?
  public open expect fun new(): swiftPMImport/emptyxcode/ADJConfig?
  public open expect fun initSdk(adjustConfig: swiftPMImport/emptyxcode/ADJConfig?): kotlin/Unit
            """.trimIndent()
        ),
        ktSnippet = """
            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            fun adjustConfig() = swiftPMImport.emptyxcode.ADJConfig(appToken = "token", environment = "env")
        """.trimIndent(),
        swiftSnippet = """
            import SwiftUI
            import Shared
            import AdjustSdk

            @main
            struct iOSApp: App {
                var body: some Scene {
                    WindowGroup {
                        let config = TempKt.adjustConfig()
                        let sdk = Adjust.initSdk(config)
                        Text("OK")
                    }
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) {
        `package`(
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
            "initWithAuthorizationEndpoint" to """@kotlinx/cinterop/ObjCConstructor(initSelector = "initWithAuthorizationEndpoint:tokenEndpoint:", designated = true)
@kotlinx/cinterop/ObjCConstructor(initSelector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:", designated = true)
@kotlinx/cinterop/ObjCConstructor(initSelector = "initWithAuthorizationEndpoint:tokenEndpoint:registrationEndpoint:", designated = true)
@kotlinx/cinterop/ObjCConstructor(initSelector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:", designated = true)
@kotlinx/cinterop/ObjCConstructor(initSelector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:endSessionEndpoint:", designated = true)
@kotlinx/cinterop/ObjCMethod(encoding = "@32@0:8@16@24", selector = "initWithAuthorizationEndpoint:tokenEndpoint:", isStret = false)
public open expect fun initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL): swiftPMImport/emptyxcode/OIDServiceConfiguration
@kotlinx/cinterop/ObjCMethod(encoding = "@40@0:8@16@24@32", selector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:", isStret = false)
public open expect fun initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, issuer: platform/Foundation/NSURL?): swiftPMImport/emptyxcode/OIDServiceConfiguration
@kotlinx/cinterop/ObjCMethod(encoding = "@40@0:8@16@24@32", selector = "initWithAuthorizationEndpoint:tokenEndpoint:registrationEndpoint:", isStret = false)
public open expect fun initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, registrationEndpoint: platform/Foundation/NSURL?): swiftPMImport/emptyxcode/OIDServiceConfiguration
@kotlinx/cinterop/ObjCMethod(encoding = "@48@0:8@16@24@32@40", selector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:", isStret = false)
public open expect fun initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, issuer: platform/Foundation/NSURL?, registrationEndpoint: platform/Foundation/NSURL?): swiftPMImport/emptyxcode/OIDServiceConfiguration
@kotlinx/cinterop/ObjCMethod(encoding = "@56@0:8@16@24@32@40@48", selector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:endSessionEndpoint:", isStret = false)
public open expect fun initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, issuer: platform/Foundation/NSURL?, registrationEndpoint: platform/Foundation/NSURL?, endSessionEndpoint: platform/Foundation/NSURL?): swiftPMImport/emptyxcode/OIDServiceConfiguration
  """.trimIndent()
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
            import SwiftUI
            import Shared
            import AppAuth

            @main
            struct iOSApp: App {
                var body: some Scene {
                    WindowGroup {
                        let request = OIDAuthorizationRequest.init(configuration: TempKt.auth(), clientId: "", scopes: [], redirectURL: NSURL(string: "http://localhost") as! URL, responseType: "code", additionalParameters: ["": ""])
                        let url = request.authorizationRequestURL().absoluteString
                        if(!url.contains("example.com")) { fatalError("value from kotlin is not passed") }
                        Text("OK \(url)")
                    }
                }
            }
        """.trimIndent(),
        isStatic = isStatic
    ) {
        `package`(
            url = url("https://github.com/openid/AppAuth-iOS.git"),
            version = exact("2.0.0"),
            products = listOf(product("AppAuth"))
        )
    }

    private fun testSwiftPackageIntegration(
        version: GradleVersion,
        expectedCinteropAPIs: Map<String, String>,
        swiftSnippet: String = "",
        ktSnippet: String = "",
        isStatic: Boolean,
        configure: SwiftImportExtension.() -> Unit,
    ) {
        if (!isTeamCityRun) {
            Assumptions.assumeTrue(version >= GradleVersion.version("8.0"))
        }
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
                        configure()
                    }
                }
            }

            val swiftFile = projectPath.resolve("iosApp/iosApp/iOSApp.swift")
            if (swiftSnippet.isNotBlank()) swiftFile.writeText(swiftSnippet)

            val ktFile = kotlinSourcesDir("iosMain").createDirectories().resolve("temp.kt").createFile()
            ktFile.writeText(ktSnippet)

            testVisibleSignatures(expectedCinteropAPIs)
            testKotlinLinkage()
            testXcodeLinkage(isStatic)
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
    build(":linkReleaseFrameworkIosArm64")
    build(":linkDebugFrameworkIosSimulatorArm64")
}

@OptIn(EnvironmentalVariablesOverride::class)
private fun TestProject.testXcodeLinkage(isStatic: Boolean) {
    build(
        "integrateLinkagePackage",
        environmentVariables = EnvironmentalVariables(
            "XCODEPROJ_PATH" to projectPath.resolve("iosApp/iosApp.xcodeproj").absolutePathString()
        )
    )
    if (!isStatic) {
        addEmbedAndSignPhaseForSpmLibrary(projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj"))
    }
    buildXcodeProject(
        xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
    )
}

private fun TestProject.testVisibleSignatures(
    expectedCinteropAPIs: Map<String, String>,
) {
    build("commonizeCInterop")

    val commonizerResult = projectPath.resolve("build/classes/kotlin/commonizer/swiftPMImport")
        .listDirectoryEntries()
        .single { it.isDirectory() }
        .listDirectoryEntries()
        .single { it.isDirectory() }
        .listDirectoryEntries()
        .single { it.isDirectory() }

    val metadataDump = dumpKlibMetadata(
        commonizerResult.toFile()
    )

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

internal fun KotlinMultiplatformExtension.swiftPMDependencies(configure: SwiftImportExtension.() -> Unit) {
    (this.extensions.getByName(SwiftImportExtension.EXTENSION_NAME) as SwiftImportExtension).configure()
}

private fun addEmbedAndSignPhaseForSpmLibrary(pbxprojFile: Path) {
    var pbxprojFileContent = pbxprojFile.readText()
    val buildFileRegex = Regex(
        """/\* Begin PBXBuildFile section \*/\s+(\w+) = \{\s+isa = PBXBuildFile;\s+productRef = (\w+);\s+\};\s+/\* End PBXBuildFile section \*/"""
    )

    val match =
        buildFileRegex.find(pbxprojFileContent) ?: throw IllegalStateException("PBXBuildFile section not found or has unexpected format")

    val buildFileId = match.groupValues[1]
    val productRefId = match.groupValues[2]
    val embedFrameworksBuildFileId = "B638C8582EE09DAA00A788A3"
    val copyFilesBuildPhaseId = "B638C8592EE09DAA00A788A3"

    println("Found buildFileId: $buildFileId")
    println("Found productRefId: $productRefId")

    // 1. Replace PBXBuildFile section with PBXBuildFile and PBXCopyFilesBuildPhase
    val newBuildFileSection = """/* Begin PBXBuildFile section */
		$buildFileId /* _internal_linkage_SwiftPMImport in Frameworks */ = {isa = PBXBuildFile; productRef = $productRefId /* _internal_linkage_SwiftPMImport */; };
		$embedFrameworksBuildFileId /* _internal_linkage_SwiftPMImport in Embed Frameworks */ = {isa = PBXBuildFile; productRef = $productRefId /* _internal_linkage_SwiftPMImport */; settings = {ATTRIBUTES = (CodeSignOnCopy, ); }; };
/* End PBXBuildFile section */

/* Begin PBXCopyFilesBuildPhase section */
		$copyFilesBuildPhaseId /* Embed Frameworks */ = {
			isa = PBXCopyFilesBuildPhase;
			buildActionMask = 2147483647;
			dstPath = "";
			dstSubfolderSpec = 10;
			files = (
				$embedFrameworksBuildFileId /* _internal_linkage_SwiftPMImport in Embed Frameworks */,
			);
			name = "Embed Frameworks";
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXCopyFilesBuildPhase section */"""

    pbxprojFileContent = buildFileRegex.replace(pbxprojFileContent, newBuildFileSection)

    // 2. Add the new build phase to the target's buildPhases array
    val buildPhasesRegex = Regex(
        """(buildPhases = \(\s+(?:\w+,\s+)+)(\);)\s+(buildRules)"""
    )

    pbxprojFileContent = buildPhasesRegex.replace(pbxprojFileContent) { matchResult ->
        "${matchResult.groupValues[1]}$copyFilesBuildPhaseId,\n\t\t\t${matchResult.groupValues[2]}\n\t\t\t${matchResult.groupValues[3]}"
    }

    pbxprojFile.writeText(pbxprojFileContent)
}
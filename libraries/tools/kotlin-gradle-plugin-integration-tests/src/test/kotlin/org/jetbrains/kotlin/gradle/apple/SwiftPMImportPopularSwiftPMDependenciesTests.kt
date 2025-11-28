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
import kotlin.collections.mapOf
import kotlin.time.measureTime

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@NativeGradlePluginTests
class SwiftPMImportPopularSwiftPMDependenciesTests : KGPBaseTest() {

    @GradleTest
    fun `direct dependency on Firebase`(version: GradleVersion) = testVisibleSignatures(
        version = version,
        expectedCinteropAPIs = mapOf(
            "firestoreForApp" to """
@kotlinx/cinterop/ObjCMethod(encoding = "@24@0:8@16", selector = "firestoreForApp:", isStret = false)
public open expect fun firestoreForApp(app: swiftPMImport/empty/FIRApp): swiftPMImport/empty/FIRFirestore
@kotlinx/cinterop/ObjCMethod(encoding = "@32@0:8@16@24", selector = "firestoreForApp:database:", isStret = false)
public open expect fun firestoreForApp(app: swiftPMImport/empty/FIRApp, database: kotlin/String): swiftPMImport/empty/FIRFirestore
                """.trimIndent(),
            "FIRAnalytics" to """
// class name: swiftPMImport/empty/FIRAnalytics
// class name: swiftPMImport/empty/FIRAnalytics.Companion
// class name: swiftPMImport/empty/FIRAnalyticsMeta
public open expect class swiftPMImport/empty/FIRAnalytics : platform/darwin/NSObject {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "FIRAnalytics()"))
  public open expect fun init(): swiftPMImport/empty/FIRAnalytics?
public final expect companion object swiftPMImport/empty/FIRAnalytics.Companion : swiftPMImport/empty/FIRAnalyticsMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/empty/FIRAnalytics> {
public open expect class swiftPMImport/empty/FIRAnalyticsMeta : platform/darwin/NSObjectMeta {
  public open expect fun alloc(): swiftPMImport/empty/FIRAnalytics?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/empty/FIRAnalytics?
  public open expect fun new(): swiftPMImport/empty/FIRAnalytics?
  public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.handleEventsForBackgroundURLSession(identifier: kotlin/String, completionHandler: kotlin/Function0<kotlin/Unit>?): kotlin/Unit
  public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.handleOpenURL(url: platform/Foundation/NSURL): kotlin/Unit
  public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.handleUserActivity(userActivity: kotlin/Any): kotlin/Unit
  public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithEmailAddress(emailAddress: kotlin/String): kotlin/Unit
  public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithHashedEmailAddress(hashedEmailAddress: platform/Foundation/NSData): kotlin/Unit
  public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithHashedPhoneNumber(hashedPhoneNumber: platform/Foundation/NSData): kotlin/Unit
  public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.initiateOnDeviceConversionMeasurementWithPhoneNumber(phoneNumber: kotlin/String): kotlin/Unit
  public final expect fun swiftPMImport/empty/FIRAnalyticsMeta.setConsent(consentSettings: kotlin/collections/Map<kotlin/Any?, *>): kotlin/Unit
                """.trimIndent()
        )
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

    @GradleTest
    fun `direct dependency on GoogleMaps`(version: GradleVersion) = testVisibleSignatures(
        version = version,
        expectedCinteropAPIs = mapOf(
            "provideAPIKey" to """
                @kotlinx/cinterop/ObjCMethod(encoding = "B24@0:8@16", selector = "provideAPIKey:", isStret = false)
                public open expect fun provideAPIKey(APIKey: kotlin/String): kotlin/Boolean
                """.trimIndent()
        )
    ) {
        iosDeploymentVersion.set("16.0")
        `package`(
            url = url("git@github.com:googlemaps/ios-maps-sdk.git"),
            version = exact("10.6.0"),
            products = listOf(product("GoogleMaps")),
        )
    }

    @GradleTest
    fun `direct dependency on Sentry`(version: GradleVersion) = testVisibleSignatures(
        version = version,
        expectedCinteropAPIs = mapOf(
            "startWithConfigureOptions" to """
                @kotlinx/cinterop/ObjCMethod(encoding = "v24@0:8@?16", selector = "startWithConfigureOptions:", isStret = false)
                public open expect fun startWithConfigureOptions(configureOptions: kotlin/Function1<swiftPMImport/empty/SentryOptions?, kotlin/Unit>): kotlin/Unit
                """.trimIndent()
        )
    ) {
        `package`(
            url = url("https://github.com/getsentry/sentry-cocoa.git"),
            version = exact("9.0.0-rc.1"), // use rc to get the fix: https://github.com/getsentry/sentry-cocoa/pull/6607
            products = listOf(product("Sentry")),
        )
    }

    @GradleTest
    fun `direct dependency on RevenueCat`(version: GradleVersion) = testVisibleSignatures(
        version = version,
        expectedCinteropAPIs = mapOf(
            "purchaseProduct" to """@kotlinx/cinterop/ObjCMethod(encoding = "v32@0:8@16@?24", selector = "purchaseProduct:withCompletion:", isStret = false)
public open expect fun purchaseProduct(product: swiftPMImport/empty/RCStoreProduct, withCompletion: kotlin/Function4<swiftPMImport/empty/RCStoreTransaction?, swiftPMImport/empty/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v40@0:8@16@24@?32", selector = "purchaseProduct:withPromotionalOffer:completion:", isStret = false)
public open expect fun purchaseProduct(product: swiftPMImport/empty/RCStoreProduct, withPromotionalOffer: swiftPMImport/empty/RCPromotionalOffer, completion: kotlin/Function4<swiftPMImport/empty/RCStoreTransaction?, swiftPMImport/empty/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v32@0:8@16@?24", selector = "purchaseProduct:withCompletion:", isStret = false)
public abstract expect fun purchaseProduct(product: swiftPMImport/empty/RCStoreProduct, withCompletion: kotlin/Function4<swiftPMImport/empty/RCStoreTransaction?, swiftPMImport/empty/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v40@0:8@16@24@?32", selector = "purchaseProduct:withPromotionalOffer:completion:", isStret = false)
public abstract expect fun purchaseProduct(product: swiftPMImport/empty/RCStoreProduct, withPromotionalOffer: swiftPMImport/empty/RCPromotionalOffer, completion: kotlin/Function4<swiftPMImport/empty/RCStoreTransaction?, swiftPMImport/empty/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v32@0:8@16@?24", selector = "purchaseProduct:withCompletionBlock:", isStret = false)
public final expect fun swiftPMImport/empty/RCPurchases.purchaseProduct(product: objcnames/classes/SKProduct, withCompletionBlock: kotlin/Function4<swiftPMImport/empty/RCStoreTransaction?, swiftPMImport/empty/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v32@0:8@16@?24", selector = "purchaseProduct:withCompletion:", isStret = false)
public final expect fun swiftPMImport/empty/RCPurchases.purchaseProduct(product: swiftPMImport/empty/RCStoreProduct, withCompletion: kotlin/Function4<swiftPMImport/empty/RCStoreTransaction?, swiftPMImport/empty/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v40@0:8@16@24@?32", selector = "purchaseProduct:withDiscount:completionBlock:", isStret = false)
public final expect fun swiftPMImport/empty/RCPurchases.purchaseProduct(product: objcnames/classes/SKProduct, withDiscount: objcnames/classes/SKPaymentDiscount, completionBlock: kotlin/Function4<swiftPMImport/empty/RCStoreTransaction?, swiftPMImport/empty/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
@kotlinx/cinterop/ObjCMethod(encoding = "v40@0:8@16@24@?32", selector = "purchaseProduct:withPromotionalOffer:completion:", isStret = false)
public final expect fun swiftPMImport/empty/RCPurchases.purchaseProduct(product: swiftPMImport/empty/RCStoreProduct, withPromotionalOffer: swiftPMImport/empty/RCPromotionalOffer, completion: kotlin/Function4<swiftPMImport/empty/RCStoreTransaction?, swiftPMImport/empty/RCCustomerInfo?, platform/Foundation/NSError?, kotlin/Boolean, kotlin/Unit>): kotlin/Unit
                """.trimIndent()
        )
    ) {
        `package`(
            url = url("https://github.com/RevenueCat/purchases-ios-spm.git"),
            version = exact("5.49.0"),
            products = listOf(product("RevenueCat")),
        )
    }

    @GradleTest
    fun `direct dependency on AWS`(version: GradleVersion) = testVisibleSignatures(
        version = version,
        expectedCinteropAPIs = mapOf(
            "defaultEC2" to """@kotlinx/cinterop/ObjCMethod(encoding = "@16@0:8", selector = "defaultEC2", isStret = false)
public open expect fun defaultEC2(): swiftPMImport/empty/AWSEC2
                """.trimIndent(),
            "AWSEC2DescribeInstancesRequest" to """// class name: swiftPMImport/empty/AWSEC2DescribeInstancesRequest
// class name: swiftPMImport/empty/AWSEC2DescribeInstancesRequest.Companion
// class name: swiftPMImport/empty/AWSEC2DescribeInstancesRequestMeta
  public open expect fun describeInstances(request: swiftPMImport/empty/AWSEC2DescribeInstancesRequest): swiftPMImport/empty/AWSTask
  public open expect fun describeInstances(request: swiftPMImport/empty/AWSEC2DescribeInstancesRequest, completionHandler: kotlin/Function2<swiftPMImport/empty/AWSEC2DescribeInstancesResult?, platform/Foundation/NSError?, kotlin/Unit>?): kotlin/Unit
public open expect class swiftPMImport/empty/AWSEC2DescribeInstancesRequest : swiftPMImport/empty/AWSRequest {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "AWSEC2DescribeInstancesRequest()"))
  public open expect fun init(): swiftPMImport/empty/AWSEC2DescribeInstancesRequest?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "AWSEC2DescribeInstancesRequest(coder)"))
  public open expect fun initWithCoder(coder: platform/Foundation/NSCoder): swiftPMImport/empty/AWSEC2DescribeInstancesRequest?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "AWSEC2DescribeInstancesRequest(dictionaryValue, error)"))
  public open expect fun initWithDictionary(dictionaryValue: kotlin/collections/Map<kotlin/Any?, *>?, error: kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCObjectVar<platform/Foundation/NSError?>>?): swiftPMImport/empty/AWSEC2DescribeInstancesRequest?
public final expect companion object swiftPMImport/empty/AWSEC2DescribeInstancesRequest.Companion : swiftPMImport/empty/AWSEC2DescribeInstancesRequestMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/empty/AWSEC2DescribeInstancesRequest> {
public open expect class swiftPMImport/empty/AWSEC2DescribeInstancesRequestMeta : swiftPMImport/empty/AWSRequestMeta {
  public open expect fun alloc(): swiftPMImport/empty/AWSEC2DescribeInstancesRequest?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/empty/AWSEC2DescribeInstancesRequest?
  public open expect fun modelWithDictionary(dictionaryValue: kotlin/collections/Map<kotlin/Any?, *>?, error: kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCObjectVar<platform/Foundation/NSError?>>?): swiftPMImport/empty/AWSEC2DescribeInstancesRequest?
  public open expect fun new(): swiftPMImport/empty/AWSEC2DescribeInstancesRequest?
            """.trimIndent(),
            "AWSEC2ImageAttributeName" to """// class name: swiftPMImport/empty/AWSEC2ImageAttributeName
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBlockDeviceMapping
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBootMode
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDeregistrationProtection
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDescription
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameImdsSupport
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameKernel
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLastLaunchedTime
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLaunchPermission
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameProductCodes
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameRAMDisk
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameSriovNetSupport
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameTpmSupport
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUefiData
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUnknown
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.Companion
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.Var
// class name: swiftPMImport/empty/AWSEC2ImageAttributeName.Var.Companion
  public open expect fun attribute(): swiftPMImport/empty/AWSEC2ImageAttributeName
  public open expect fun setAttribute(attribute: swiftPMImport/empty/AWSEC2ImageAttributeName): kotlin/Unit
  public final expect var attribute: swiftPMImport/empty/AWSEC2ImageAttributeName
public final expect enum class swiftPMImport/empty/AWSEC2ImageAttributeName : kotlin/Enum<swiftPMImport/empty/AWSEC2ImageAttributeName>, kotlinx/cinterop/CEnum {
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
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBlockDeviceMapping : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameBootMode : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDeregistrationProtection : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameDescription : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameImdsSupport : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameKernel : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLastLaunchedTime : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameLaunchPermission : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameProductCodes : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameRAMDisk : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameSriovNetSupport : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameTpmSupport : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUefiData : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect enum entry swiftPMImport/empty/AWSEC2ImageAttributeName.AWSEC2ImageAttributeNameUnknown : swiftPMImport/empty/AWSEC2ImageAttributeName {
public final expect companion object swiftPMImport/empty/AWSEC2ImageAttributeName.Companion : kotlin/Any {
  public final expect fun byValue(value: kotlin/Long /* = platform/darwin/NSInteger^ */): swiftPMImport/empty/AWSEC2ImageAttributeName
public final expect class swiftPMImport/empty/AWSEC2ImageAttributeName.Var : kotlinx/cinterop/CEnumVar {
  public final expect var value: swiftPMImport/empty/AWSEC2ImageAttributeName
public final expect companion object swiftPMImport/empty/AWSEC2ImageAttributeName.Var.Companion : kotlinx/cinterop/CPrimitiveVar.Type {
            """.trimIndent(),
            "AWSS3TransferUtilityDownloadTask" to """// class name: swiftPMImport/empty/AWSS3TransferUtilityDownloadTask
// class name: swiftPMImport/empty/AWSS3TransferUtilityDownloadTask.Companion
// class name: swiftPMImport/empty/AWSS3TransferUtilityDownloadTaskMeta
  public open expect fun downloadDataForKey(key: kotlin/String, expression: swiftPMImport/empty/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): swiftPMImport/empty/AWSTask
  public open expect fun downloadDataFromBucket(bucket: kotlin/String, key: kotlin/String, expression: swiftPMImport/empty/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): swiftPMImport/empty/AWSTask
  public open expect fun downloadToURL(fileURL: platform/Foundation/NSURL, key: kotlin/String, expression: swiftPMImport/empty/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): swiftPMImport/empty/AWSTask
  public open expect fun downloadToURL(fileURL: platform/Foundation/NSURL, bucket: kotlin/String, key: kotlin/String, expression: swiftPMImport/empty/AWSS3TransferUtilityDownloadExpression?, completionHandler: kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): swiftPMImport/empty/AWSTask
  public open expect fun enumerateToAssignBlocksForUploadTask(uploadBlocksAssigner: kotlin/Function3<swiftPMImport/empty/AWSS3TransferUtilityUploadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlock^? */> /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlockVar^ */>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityUploadCompletionHandlerBlock^? */> /* = swiftPMImport/empty/AWSS3TransferUtilityUploadCompletionHandlerBlockVar^ */>?, kotlin/Unit>?, downloadTask: kotlin/Function3<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlock^? */> /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlockVar^ */>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */> /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlockVar^ */>?, kotlin/Unit>?): kotlin/Unit
  public open expect fun enumerateToAssignBlocksForUploadTask(uploadBlocksAssigner: kotlin/Function3<swiftPMImport/empty/AWSS3TransferUtilityUploadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlock^? */> /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlockVar^ */>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityUploadCompletionHandlerBlock^? */> /* = swiftPMImport/empty/AWSS3TransferUtilityUploadCompletionHandlerBlockVar^ */>?, kotlin/Unit>, multiPartUploadBlocksAssigner: kotlin/Function3<swiftPMImport/empty/AWSS3TransferUtilityMultiPartUploadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityMultiPartProgressBlock^? */> /* = swiftPMImport/empty/AWSS3TransferUtilityMultiPartProgressBlockVar^ */>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityMultiPartUploadCompletionHandlerBlock^? */> /* = swiftPMImport/empty/AWSS3TransferUtilityMultiPartUploadCompletionHandlerBlockVar^ */>?, kotlin/Unit>, downloadBlocksAssigner: kotlin/Function3<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlock^? */> /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlockVar^ */>?, kotlinx/cinterop/CPointer<kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */> /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlockVar^ */>?, kotlin/Unit>): kotlin/Unit
  public /* secondary */ constructor(uploadProgress: kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlock^? */, multiPartUploadProgress: kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityMultiPartProgressBlock^? */, downloadProgress: kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlock^? */, uploadCompleted: kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityUploadCompletionHandlerBlock^? */, multiPartUploadCompleted: kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityMultiPartUploadCompletionHandlerBlock^? */, downloadCompleted: kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */)
  public open expect fun downloadCompletedBlock(): kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */
  public open expect fun initWithUploadProgress(uploadProgressBlock: kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlock^? */, multiPartUploadProgress: kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityMultiPartProgressBlock^? */, downloadProgress: kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityTask?, platform/Foundation/NSProgress?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityProgressBlock^? */, uploadCompleted: kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityUploadCompletionHandlerBlock^? */, multiPartUploadCompleted: kotlin/Function2<swiftPMImport/empty/AWSS3TransferUtilityMultiPartUploadTask?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityMultiPartUploadCompletionHandlerBlock^? */, downloadCompleted: kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): swiftPMImport/empty/AWSS3TransferUtilityBlocks
  public final expect val downloadCompletedBlock: kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */
public open expect class swiftPMImport/empty/AWSS3TransferUtilityDownloadTask : swiftPMImport/empty/AWSS3TransferUtilityTask {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "AWSS3TransferUtilityDownloadTask()"))
  public open expect fun init(): swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?
  public open expect fun setCompletionHandler(completionHandler: kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */): kotlin/Unit
public final expect companion object swiftPMImport/empty/AWSS3TransferUtilityDownloadTask.Companion : swiftPMImport/empty/AWSS3TransferUtilityDownloadTaskMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask> {
public open expect class swiftPMImport/empty/AWSS3TransferUtilityDownloadTaskMeta : swiftPMImport/empty/AWSS3TransferUtilityTaskMeta {
  public open expect fun alloc(): swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?
  public open expect fun new(): swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?
  public typealias AWSS3TransferUtilityDownloadCompletionHandlerBlock = kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? */
  public typealias AWSS3TransferUtilityDownloadCompletionHandlerBlockVar = kotlinx/cinterop/ObjCBlockVar^<swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^?> /* = kotlinx/cinterop/ObjCNotImplementedVar<kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>?> /* = kotlinx/cinterop/ObjCBlockVar^<kotlin/Function4<swiftPMImport/empty/AWSS3TransferUtilityDownloadTask?, platform/Foundation/NSURL?, platform/Foundation/NSData?, platform/Foundation/NSError?, kotlin/Unit>? /* = swiftPMImport/empty/AWSS3TransferUtilityDownloadCompletionHandlerBlock^? */> */ */
  """.trimIndent()
        )
    ) {
        `package`(
            url = url("https://github.com/aws-amplify/aws-sdk-ios-spm.git"),
            version = exact("2.41.0"),
            products = listOf(product("AWSS3"), product("AWSEC2")),
        )
    }

    @GradleTest
    fun `direct dependency on Mapbox`(version: GradleVersion) = testVisibleSignatures(
        version = version,
        expectedCinteropAPIs = mapOf(
            "MapView" to """// class name: swiftPMImport/empty/MapView
// class name: swiftPMImport/empty/MapView.Companion
// class name: swiftPMImport/empty/MapViewMeta
public open expect class swiftPMImport/empty/MapView : platform/UIKit/UIView {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "MapView()"))
  public open expect fun init(): swiftPMImport/empty/MapView
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "MapView(coder)"))
  public open expect fun initWithCoder(coder: platform/Foundation/NSCoder): swiftPMImport/empty/MapView?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "MapView(frame)"))
  public open expect fun initWithFrame(frame: kotlinx/cinterop/CValue<platform/CoreGraphics/CGRect>): swiftPMImport/empty/MapView
public final expect companion object swiftPMImport/empty/MapView.Companion : swiftPMImport/empty/MapViewMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/empty/MapView> {
public open expect class swiftPMImport/empty/MapViewMeta : platform/UIKit/UIViewMeta {
  public open expect fun alloc(): swiftPMImport/empty/MapView?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/empty/MapView?
  public open expect fun appearance(): swiftPMImport/empty/MapView
  public open expect fun appearanceForTraitCollection(trait: platform/UIKit/UITraitCollection): swiftPMImport/empty/MapView
  public open expect fun appearanceForTraitCollection(trait: platform/UIKit/UITraitCollection, whenContainedInInstancesOfClasses: kotlin/collections/List<*>): swiftPMImport/empty/MapView
  public open expect fun appearanceForTraitCollection(trait: platform/UIKit/UITraitCollection, whenContainedIn: platform/UIKit/UIAppearanceContainerProtocol?, vararg args: kotlin/Any? /* kotlin/Array<kotlin/Any?> */): swiftPMImport/empty/MapView
  public open expect fun appearanceWhenContainedIn(ContainerClass: platform/UIKit/UIAppearanceContainerProtocol?, vararg args: kotlin/Any? /* kotlin/Array<kotlin/Any?> */): swiftPMImport/empty/MapView
  public open expect fun appearanceWhenContainedInInstancesOfClasses(containerTypes: kotlin/collections/List<*>): swiftPMImport/empty/MapView
  public open expect fun new(): swiftPMImport/empty/MapView?
  """.trimIndent()
        )
    ) {
        `package`(
            url = url("https://github.com/mapbox/mapbox-maps-ios.git"),
            version = exact("11.16.6"),
            products = listOf(product("MapboxMaps")),
        )
    }

    @GradleTest
    fun `direct dependency on Tun2SocksKit`(version: GradleVersion) = testVisibleSignatures(
        version = version,
        expectedCinteropAPIs = mapOf(
            "socks5_tunnel" to """public final expect fun hev_socks5_tunnel_main(config_path: kotlin/String?, tun_fd: kotlin/Int): kotlin/Int
public final expect fun hev_socks5_tunnel_main_from_file(config_path: kotlin/String?, tun_fd: kotlin/Int): kotlin/Int
public final expect fun hev_socks5_tunnel_main_from_str(config_str: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/UByteVarOf<kotlin/UByte> /* = kotlinx/cinterop/UByteVar^ */>?, config_len: kotlin/UInt, tun_fd: kotlin/Int): kotlin/Int
public final expect fun hev_socks5_tunnel_quit(): kotlin/Unit
public final expect fun hev_socks5_tunnel_stats(tx_packets: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong /* = platform/posix/size_t^ */> /* = platform/posix/size_tVar^ */>?, tx_bytes: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong /* = platform/posix/size_t^ */> /* = platform/posix/size_tVar^ */>?, rx_packets: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong /* = platform/posix/size_t^ */> /* = platform/posix/size_tVar^ */>?, rx_bytes: kotlinx/cinterop/CValuesRef<kotlinx/cinterop/ULongVarOf<kotlin/ULong /* = platform/posix/size_t^ */> /* = platform/posix/size_tVar^ */>?): kotlin/Unit
  """.trimIndent()
        )
    ) {
        `package`(
            url = url("https://github.com/EbrahimTahernejad/Tun2SocksKit.git"),
            version = exact("5.14.1"),
            products = listOf(product("Tun2SocksKit"))
        )
    }

    @GradleTest
    fun `direct dependency on Datadog`(version: GradleVersion) = testVisibleSignatures(
        version = version,
        expectedCinteropAPIs = mapOf(
            "Datadog" to """// class name: swiftPMImport/empty/DDDatadog
// class name: swiftPMImport/empty/DDDatadog.Companion
// class name: swiftPMImport/empty/DDDatadogMeta
public open expect class swiftPMImport/empty/DDDatadog : platform/darwin/NSObject {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "DDDatadog()"))
  public open expect fun init(): swiftPMImport/empty/DDDatadog
public final expect companion object swiftPMImport/empty/DDDatadog.Companion : swiftPMImport/empty/DDDatadogMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/empty/DDDatadog> {
public open expect class swiftPMImport/empty/DDDatadogMeta : platform/darwin/NSObjectMeta {
  public open expect fun alloc(): swiftPMImport/empty/DDDatadog?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/empty/DDDatadog?
  public open expect fun new(): swiftPMImport/empty/DDDatadog?
  """.trimIndent(),
            "DDLogs" to """// class name: swiftPMImport/empty/DDLogs
// class name: swiftPMImport/empty/DDLogs.Companion
// class name: swiftPMImport/empty/DDLogsConfiguration
// class name: swiftPMImport/empty/DDLogsConfiguration.Companion
// class name: swiftPMImport/empty/DDLogsConfigurationMeta
// class name: swiftPMImport/empty/DDLogsMeta
public open expect class swiftPMImport/empty/DDLogs : platform/darwin/NSObject {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "DDLogs()"))
  public open expect fun init(): swiftPMImport/empty/DDLogs
public final expect companion object swiftPMImport/empty/DDLogs.Companion : swiftPMImport/empty/DDLogsMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/empty/DDLogs> {
public open expect class swiftPMImport/empty/DDLogsConfiguration : platform/darwin/NSObject {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "DDLogsConfiguration()"))
  public open expect fun init(): swiftPMImport/empty/DDLogsConfiguration?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "DDLogsConfiguration(customEndpoint)"))
  public open expect fun initWithCustomEndpoint(customEndpoint: platform/Foundation/NSURL?): swiftPMImport/empty/DDLogsConfiguration
public final expect companion object swiftPMImport/empty/DDLogsConfiguration.Companion : swiftPMImport/empty/DDLogsConfigurationMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/empty/DDLogsConfiguration> {
public open expect class swiftPMImport/empty/DDLogsConfigurationMeta : platform/darwin/NSObjectMeta {
  public open expect fun alloc(): swiftPMImport/empty/DDLogsConfiguration?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/empty/DDLogsConfiguration?
  public open expect fun new(): swiftPMImport/empty/DDLogsConfiguration?
public open expect class swiftPMImport/empty/DDLogsMeta : platform/darwin/NSObjectMeta {
  public open expect fun alloc(): swiftPMImport/empty/DDLogs?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/empty/DDLogs?
  public open expect fun enableWith(configuration: swiftPMImport/empty/DDLogsConfiguration): kotlin/Unit
  public open expect fun new(): swiftPMImport/empty/DDLogs?
            """.trimIndent()
        )
    ) {
        `package`(
            url = url("https://github.com/DataDog/dd-sdk-ios.git"),
            version = exact("3.3.0"),
            products = listOf(product("DatadogCore"), product("DatadogLogs")),
        )
    }

    @GradleTest
    fun `direct dependency on AdjustSDK`(version: GradleVersion) = testVisibleSignatures(
        version = version,
        expectedCinteropAPIs = mapOf(
            "WKWebView" to """@kotlinx/cinterop/ObjCMethod(encoding = "v24@0:8@16", selector = "loadWKWebViewBridge:", isStret = false)
public open expect fun loadWKWebViewBridge(wkWebView: platform/WebKit/WKWebView): kotlin/Unit
public open expect fun setWkWebView(wkWebView: platform/WebKit/WKWebView): kotlin/Unit
public open expect fun wkWebView(): platform/WebKit/WKWebView
public final expect var wkWebView: platform/WebKit/WKWebView
  """.trimIndent(),
            "ADJConfig" to """// class name: swiftPMImport/empty/ADJConfig
// class name: swiftPMImport/empty/ADJConfig.Companion
// class name: swiftPMImport/empty/ADJConfigMeta
public open expect class swiftPMImport/empty/ADJConfig : platform/darwin/NSObject, platform/Foundation/NSCopyingProtocol {
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "ADJConfig()"))
  public open expect fun init(): swiftPMImport/empty/ADJConfig?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "ADJConfig(appToken, environment)"))
  public open expect fun initWithAppToken(appToken: kotlin/String, environment: kotlin/String): swiftPMImport/empty/ADJConfig?
  @kotlin/Deprecated(level = kotlin/DeprecationLevel.ERROR, message = "Use constructor instead", replaceWith = kotlin/ReplaceWith(imports = [], expression = "ADJConfig(appToken, environment, suppressLogLevel)"))
  public open expect fun initWithAppToken(appToken: kotlin/String, environment: kotlin/String, suppressLogLevel: kotlin/Boolean): swiftPMImport/empty/ADJConfig?
public final expect companion object swiftPMImport/empty/ADJConfig.Companion : swiftPMImport/empty/ADJConfigMeta, kotlinx/cinterop/ObjCClassOf<swiftPMImport/empty/ADJConfig> {
public open expect class swiftPMImport/empty/ADJConfigMeta : platform/darwin/NSObjectMeta, platform/Foundation/NSCopyingProtocolMeta {
  public open expect fun alloc(): swiftPMImport/empty/ADJConfig?
  public open expect fun allocWithZone(zone: kotlinx/cinterop/CPointer<cnames/structs/_NSZone>?): swiftPMImport/empty/ADJConfig?
  public open expect fun new(): swiftPMImport/empty/ADJConfig?
  public open expect fun initSdk(adjustConfig: swiftPMImport/empty/ADJConfig?): kotlin/Unit
            """.trimIndent()
        )
    ) {
        `package`(
            url = url("https://github.com/adjust/ios_sdk.git"),
            version = exact("5.4.6"),
            products = listOf(product("AdjustWebBridge"))
        )
    }

    @GradleTest
    fun `direct dependency on AppAuth`(version: GradleVersion) = testVisibleSignatures(
        version = version,
        expectedCinteropAPIs = mapOf(
            "initWithAuthorizationEndpoint" to """@kotlinx/cinterop/ObjCConstructor(initSelector = "initWithAuthorizationEndpoint:tokenEndpoint:", designated = true)
@kotlinx/cinterop/ObjCConstructor(initSelector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:", designated = true)
@kotlinx/cinterop/ObjCConstructor(initSelector = "initWithAuthorizationEndpoint:tokenEndpoint:registrationEndpoint:", designated = true)
@kotlinx/cinterop/ObjCConstructor(initSelector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:", designated = true)
@kotlinx/cinterop/ObjCConstructor(initSelector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:endSessionEndpoint:", designated = true)
@kotlinx/cinterop/ObjCMethod(encoding = "@32@0:8@16@24", selector = "initWithAuthorizationEndpoint:tokenEndpoint:", isStret = false)
public open expect fun initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL): swiftPMImport/empty/OIDServiceConfiguration
@kotlinx/cinterop/ObjCMethod(encoding = "@40@0:8@16@24@32", selector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:", isStret = false)
public open expect fun initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, issuer: platform/Foundation/NSURL?): swiftPMImport/empty/OIDServiceConfiguration
@kotlinx/cinterop/ObjCMethod(encoding = "@40@0:8@16@24@32", selector = "initWithAuthorizationEndpoint:tokenEndpoint:registrationEndpoint:", isStret = false)
public open expect fun initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, registrationEndpoint: platform/Foundation/NSURL?): swiftPMImport/empty/OIDServiceConfiguration
@kotlinx/cinterop/ObjCMethod(encoding = "@48@0:8@16@24@32@40", selector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:", isStret = false)
public open expect fun initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, issuer: platform/Foundation/NSURL?, registrationEndpoint: platform/Foundation/NSURL?): swiftPMImport/empty/OIDServiceConfiguration
@kotlinx/cinterop/ObjCMethod(encoding = "@56@0:8@16@24@32@40@48", selector = "initWithAuthorizationEndpoint:tokenEndpoint:issuer:registrationEndpoint:endSessionEndpoint:", isStret = false)
public open expect fun initWithAuthorizationEndpoint(authorizationEndpoint: platform/Foundation/NSURL, tokenEndpoint: platform/Foundation/NSURL, issuer: platform/Foundation/NSURL?, registrationEndpoint: platform/Foundation/NSURL?, endSessionEndpoint: platform/Foundation/NSURL?): swiftPMImport/empty/OIDServiceConfiguration
  """.trimIndent()
        )
    ) {
        `package`(
            url = url("https://github.com/openid/AppAuth-iOS.git"),
            version = exact("2.0.0"),
            products = listOf(product("AppAuth"))
        )
    }


    private fun testVisibleSignatures(
        version: GradleVersion,
        expectedCinteropAPIs: Map<String, String>,
        configure: SwiftImportExtension.() -> Unit,
    ) {
        if (!isTeamCityRun) {
            Assumptions.assumeTrue(version >= GradleVersion.version("8.0"))
        }
        project("empty", version) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosSimulatorArm64()

                    swiftPMDependencies {
                        configure()
                    }
                }
            }

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
            measureTime {
                metadataDump.lines().forEach { line ->
                    expectedCinteropAPIs.keys.forEach { key ->
                        if (key in line) {
                            actualSignatures.getOrPut(key) { mutableListOf() }.add(line)
                        }
                    }
                }
            }.also {
                println(it.inWholeMilliseconds)
            }

            assertEquals(
                expectedCinteropAPIs.prettyPrinted,
                actualSignatures.mapValues { it.value.joinToString("\n").trimIndent() }.prettyPrinted,
            )
        }
    }
}

internal fun KotlinMultiplatformExtension.swiftPMDependencies(configure: SwiftImportExtension.() -> Unit) {
    (this.extensions.getByName(SwiftImportExtension.EXTENSION_NAME) as SwiftImportExtension).configure()
}
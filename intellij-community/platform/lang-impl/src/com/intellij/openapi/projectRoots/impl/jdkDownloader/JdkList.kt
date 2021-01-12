// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.collect.ImmutableList
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import com.intellij.util.lang.JavaVersion
import org.jetbrains.annotations.NonNls
import org.jetbrains.jps.model.java.JdkVersionDetector
import org.tukaani.xz.XZInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/** describes vendor + product part of the UI **/
data class JdkProduct(
  val vendor: String,
  val product: String?,
  val flavour: String?
) {
  val packagePresentationText: String
    get() = buildString {
      append(vendor)
      if (product != null) {
        append(" ")
        append(product)
      }

      if (flavour != null) {
        append(" (")
        append(flavour)
        append(")")
      }
    }
}

/** describes an item behind the version as well as download info **/
data class JdkItem(
  val product: JdkProduct,

  val isDefaultItem: Boolean = false,

  /** there are some JdkList items that are not shown in the downloader but suggested for JdkAuto **/
  val isVisibleOnUI: Boolean,

  val jdkMajorVersion: Int,
  val jdkVersion: String,
  private val jdkVendorVersion: String?,
  val suggestedSdkName: String,

  val arch: String,
  val packageType: JdkPackageType,
  val url: String,
  val sha256: String,

  val archiveSize: Long,
  val unpackedSize: Long,

  // we should only extract items that has the given prefix removing the prefix
  val packageRootPrefix: String,
  // the path from the package root to the java home directory (where bin/java is)
  val packageToBinJavaPrefix: String,

  val archiveFileName: String,
  val installFolderName: String,

  val sharedIndexAliases: List<String>,

  private val saveToFile: (File) -> Unit
) {

  fun writeMarkerFile(file: File) {
    saveToFile(file)
  }

  val vendorPrefix
    get() = suggestedSdkName.split("-").dropLast(1).joinToString("-")

  fun matchesVendor(predicate: String) : Boolean {
    val cases = sequence {
      yield(product.vendor)

      yield(vendorPrefix)
      if (product.product != null) {
        yield(product.product)
        yield("${product.vendor}-${product.product}")
        if (product.flavour != null) {
          yield("${product.product}-${product.flavour}")
          yield("${product.vendor}-${product.product}-${product.flavour}")
        }
      }
    }

    val match = predicate.trim()
    return cases.any { it.equals(match, ignoreCase = true) }
  }

  /**
   * Returns versionString for the Java Sdk object in specific format
   */
  val versionString
    get() = JavaVersion.tryParse(jdkVersion)?.let(JdkVersionDetector::formatVersionString) ?: jdkVersion

  val presentableVersionString
    get() = JavaVersion.tryParse(jdkVersion)?.toFeatureMinorUpdateString() ?: jdkVersion

  val versionPresentationText: String
    get() = jdkVersion

  val downloadSizePresentationText: String
    get() = StringUtil.formatFileSize(archiveSize)

  val fullPresentationText: String
    get() = product.packagePresentationText + " " + jdkVersion
}

enum class JdkPackageType(@NonNls val type: String) {
  @Suppress("unused")
  ZIP("zip") {
    override fun openDecompressor(archiveFile: File): Decompressor {
      val decompressor = Decompressor.Zip(archiveFile)
      return when {
        SystemInfo.isWindows -> decompressor
        else -> decompressor.withUnixPermissionsAndSymlinks()
      }
    }
  },

  @Suppress("SpellCheckingInspection", "unused")
  TAR_GZ("targz") {
    override fun openDecompressor(archiveFile: File) = Decompressor.Tar(archiveFile).withSymlinks()
  };

  abstract fun openDecompressor(archiveFile: File): Decompressor

  companion object {
    fun findType(jsonText: String): JdkPackageType? = values().firstOrNull { it.type.equals(jsonText, ignoreCase = true) }
  }
}

data class JdkPredicate(
  private val ideBuildNumber: BuildNumber,
  private val expectedOS: String
) {

  companion object {
    fun createInstance(): JdkPredicate {
      val expectedOS = when {
        SystemInfo.isWindows -> "windows"
        SystemInfo.isMac -> "macOS"
        SystemInfo.isLinux -> "linux"
        else -> error("Unsupported OS")
      }
      return JdkPredicate(ApplicationInfoImpl.getShadowInstance().build, expectedOS)
    }
  }

  fun testJdkProduct(product: ObjectNode): Boolean {
    val filterNode = product["filter"]
    return testPredicate(filterNode) == true
  }

  fun testJdkPackage(pkg: ObjectNode): Boolean {
    if (pkg["os"]?.asText() != expectedOS) return false
    if (pkg["package_type"]?.asText()?.let(JdkPackageType.Companion::findType) == null) return false
    return testPredicate(pkg["filter"]) == true
  }

  /**
   * tests the predicate from the `filter` or `default` elements an JDK product
   * against current IDE instance
   *
   * returns `null` if there was something unknown detected in the filter
   *
   * It supports the following predicates with `type` equal to `build_number_range`, `and`, `or`, `not`, e.g.
   *         { "type": "build_number_range", "since": "192.34", "until": "194.123" }
   * or
   *         { "type": "or"|"and", "items": [ {same as before}, ...] }
   * or
   *         { "type": "not", "item": { same as before } }
   * or
   *         { "type": "const", "value": true | false  }
   */
  fun testPredicate(filter: JsonNode?): Boolean? {
    //no filter means predicate is true
    if (filter == null) return true

    // used in "default" element
    if (filter.isBoolean) return filter.asBoolean()

    if (filter !is ObjectNode) return null

    val type = filter["type"]?.asText() ?: return null
    if (type == "or") {
      return foldSubPredicates(filter, false, Boolean::or)
    }

    if (type == "and") {
      return foldSubPredicates(filter, true, Boolean::and)
    }

    if (type == "not") {
      val subResult = testPredicate(filter["item"]) ?: return null
      return !subResult
    }

    if (type == "const") {
      return filter["value"]?.asBoolean()
    }

    if (type == "build_number_range") {
      val fromBuild = filter["since"]?.asText()
      val untilBuild = filter["until"]?.asText()

      if (fromBuild == null && untilBuild == null) return true

      if (fromBuild != null) {
        val fromBuildSafe = BuildNumber.fromStringOrNull(fromBuild) ?: return null
        if (fromBuildSafe > ideBuildNumber) return false
      }

      if (untilBuild != null) {
        val untilBuildSafe = BuildNumber.fromStringOrNull(untilBuild) ?: return null
        if (ideBuildNumber > untilBuildSafe) return false
      }

      return true
    }

    return null
  }

  private fun foldSubPredicates(filter: ObjectNode,
                                emptyResult: Boolean,
                                op: (acc: Boolean, Boolean) -> Boolean): Boolean? {
    val items = filter["items"] as? ArrayNode ?: return null
    if (items.isEmpty) return false
    return items.fold(emptyResult) { acc, subFilter ->
      val subResult = testPredicate(subFilter) ?: return null
      op(acc, subResult)
    }
  }
}

object JdkListParser {
  fun readTree(rawData: ByteArray) = ObjectMapper().readTree(rawData) as? ObjectNode ?: error("Unexpected JSON data")

  fun parseJdkList(tree: ObjectNode, filters: JdkPredicate): List<JdkItem> {
    val items = tree["jdks"] as? ArrayNode ?: error("`jdks` element is missing")

    val result = mutableListOf<JdkItem>()
    for (item in items.filterIsInstance<ObjectNode>()) {
      result += parseJdkItem(item, filters) ?: continue
    }

    return result.toList()
  }

  fun parseJdkItem(item: ObjectNode, filters: JdkPredicate): JdkItem? {
    // check this package is OK to show for that instance of the IDE
    if (!filters.testJdkProduct(item)) return null

    val packages = item["packages"] as? ArrayNode ?: return null
    // take the first matching package
    val pkg = packages.filterIsInstance<ObjectNode>().firstOrNull(filters::testJdkPackage) ?: return null

    val product = JdkProduct(
      vendor = item["vendor"]?.asText() ?: return null,
      product = item["product"]?.asText(),
      flavour = item["flavour"]?.asText()
    )

    val contents = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(item)
    return JdkItem(product = product,
                   isDefaultItem = item["default"]?.let { filters.testPredicate(it) == true } ?: false,
                   isVisibleOnUI = item["listed"]?.let { filters.testPredicate(it) == true } ?: true,

                   jdkMajorVersion = item["jdk_version_major"]?.asInt() ?: return null,
                   jdkVersion = item["jdk_version"]?.asText() ?: return null,
                   jdkVendorVersion = item["jdk_vendor_version"]?.asText(),
                   suggestedSdkName = item["suggested_sdk_name"]?.asText() ?: return null,

                   arch = pkg["arch"]?.asText() ?: return null,
                   packageType = pkg["package_type"]?.asText()?.let(JdkPackageType.Companion::findType) ?: return null,
                   url = pkg["url"]?.asText() ?: return null,
                   sha256 = pkg["sha256"]?.asText() ?: return null,
                   archiveSize = pkg["archive_size"]?.asLong() ?: return null,
                   archiveFileName = pkg["archive_file_name"]?.asText() ?: return null,
                   packageRootPrefix = pkg["package_root_prefix"]?.asText() ?: return null,
                   packageToBinJavaPrefix = pkg["package_to_java_home_prefix"]?.asText() ?: return null,

                   unpackedSize = pkg["unpacked_size"]?.asLong() ?: return null,
                   installFolderName = pkg["install_folder_name"]?.asText() ?: return null,

                   sharedIndexAliases = (item["shared_index_aliases"] as? ArrayNode)?.mapNotNull { it.asText() } ?: listOf(),

                   saveToFile = { file -> file.writeBytes(contents) }
    )
  }
}

class JdkListDownloader {
  companion object {
    @JvmStatic
    fun getInstance() = service<JdkListDownloader>()
  }

  private val feedUrl: String
    get() {
      val registry = runCatching { Registry.get("jdk.downloader.url").asString() }.getOrNull()
      if (!registry.isNullOrBlank()) return registry
      return "https://download.jetbrains.com/jdk/feed/v1/jdks.json.xz"
    }

  private fun downloadJdkList(feedUrl: String, progress: ProgressIndicator?) =
    HttpRequests
      .request(feedUrl)
      .productNameAsUserAgent()
      //timeouts are handled inside
      .readBytes(progress)

  /**
   * Returns a list of entries for JDK automatic installation. That set of entries normally
   * contains few more entries than the result of the [downloadForUI] call.
   * Entries are sorter from the best suggested to the worst suggested items.
   */
  fun downloadModelForJdkInstaller(progress: ProgressIndicator?): List<JdkItem> {
    return downloadJdksListWithCache(feedUrl, progress)
  }

  /**
   * Lists all entries suitable for UI download, there can be some unlisted entries that are ignored here by intent
   */
  fun downloadForUI(progress: ProgressIndicator?, feedUrl: String? = null) : List<JdkItem> {
    return downloadJdksListWithCache(feedUrl, progress).filter { it.isVisibleOnUI }
  }

  private val jdksListCache = CachedValueWithTTL<List<JdkItem>>(15 to TimeUnit.MINUTES)

  private fun downloadJdksListWithCache(feedUrl: String?, progress: ProgressIndicator?): List<JdkItem> {
    @Suppress("NAME_SHADOWING")
    val feedUrl = feedUrl ?: this.feedUrl

    return jdksListCache.getOrCompute(feedUrl, listOf()) {
      downloadJdksListNoCache(feedUrl, progress)
    }
  }

  private fun downloadJdksListNoCache(feedUrl: String, progress: ProgressIndicator?): List<JdkItem> {
    // download XZ packed version of the data (several KBs packed, several dozen KBs unpacked) and process it in-memory
    val rawDataXZ = try {
      downloadJdkList(feedUrl, progress)
    }
    catch (t: IOException) {
      Logger.getInstance(javaClass).warn("Failed to download the list of available JDKs from $feedUrl. ${t.message}")
      return emptyList()
    }

    val rawData = try {
      ByteArrayInputStream(rawDataXZ).use { input ->
        XZInputStream(input).use {
          it.readBytes()
        }
      }
    }
    catch (t: Throwable) {
      throw RuntimeException("Failed to unpack the list of available JDKs from $feedUrl. ${t.message}", t)
    }

    val json = try {
      JdkListParser.readTree(rawData)
    }
    catch (t: Throwable) {
      throw RuntimeException("Failed to parse the downloaded list of available JDKs. ${t.message}", t)
    }

    try {
      return ImmutableList.copyOf(JdkListParser.parseJdkList(json, JdkPredicate.createInstance()))
    }
    catch (t: Throwable) {
      throw RuntimeException("Failed to process the downloaded list of available JDKs from $feedUrl. ${t.message}", t)
    }
  }
}

private class CachedValueWithTTL<T : Any>(
  private val ttl: Pair<Int, TimeUnit>
) {
  private val lock = ReentrantReadWriteLock()
  private var cachedUrl: String? = null
  private var value: T? = null
  private var computed = 0L

  private fun now() = System.currentTimeMillis()
  private operator fun Long.plus(ttl: Pair<Int, TimeUnit>): Long = this + ttl.second.toMillis(ttl.first.toLong())

  private inline fun readValueOrNull(expectedUrl: String, onValue: (T) -> Unit) {
    if (cachedUrl != expectedUrl) {
      return
    }

    val value = this.value
    if (value != null && computed + ttl > now()) {
      onValue(value)
    }
  }

  fun getOrCompute(url: String, defaultOrFailure: T, compute: () -> T): T {
    lock.read {
      readValueOrNull(url) { return it }
    }

    lock.write {
      //double checked
      readValueOrNull(url) { return it }

      val value = runCatching(compute).getOrElse {
        if (it is ProcessCanceledException) {
          throw it
        }
        Logger.getInstance(javaClass).warn("Failed to compute value. ${it.message}", it)
        defaultOrFailure
      }

      ProgressManager.checkCanceled()
      this.value = value
      computed = now()
      cachedUrl = url
      return value
    }
  }
}

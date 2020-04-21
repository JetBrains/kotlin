/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.ProjectTopics
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction.nonBlocking
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.util.PathUtilRt
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.kotlin.idea.caches.project.getModuleInfosFromIdeaModel
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibraryNameUtil.isGradleLibraryName
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibraryNameUtil.parseIDELibraryName
import org.jetbrains.kotlin.idea.klib.KlibCompatibilityInfo.IncompatibleMetadata
import org.jetbrains.kotlin.idea.util.application.getServiceSafe
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.idea.versions.UnsupportedAbiVersionNotificationPanelProvider
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME

// BUNCH: 192
/** TODO: merge [KotlinNativeABICompatibilityChecker] in the future with [UnsupportedAbiVersionNotificationPanelProvider], KT-34525 */
class KotlinNativeABICompatibilityChecker : StartupActivity {
    override fun runActivity(project: Project) {
        val service = KotlinNativeABICompatibilityCheckerService.getInstance(project)
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                // run when project roots are changes, e.g. on project import
                service.validateKotlinNativeLibraries()
            }
        })

        service.validateKotlinNativeLibraries()
    }
}

class KotlinNativeABICompatibilityCheckerService(private val project: Project) {

    private sealed class LibraryGroup(private val ordinal: Int) : Comparable<LibraryGroup> {

        override fun compareTo(other: LibraryGroup) = when {
            this == other -> 0
            this is FromDistribution && other is FromDistribution -> kotlinVersion.compareTo(other.kotlinVersion)
            else -> ordinal.compareTo(other.ordinal)
        }

        data class FromDistribution(val kotlinVersion: String) : LibraryGroup(0)
        object ThirdParty : LibraryGroup(1)
        object User : LibraryGroup(2)
    }

    private val cachedIncompatibleLibraries = mutableSetOf<String>()

    internal fun validateKotlinNativeLibraries() {
        if (isUnitTestMode() || project.isDisposed) return

        val backgroundJob: Ref<CancellablePromise<*>> = Ref()
        val disposable = Disposable {
            backgroundJob.get()?.let(CancellablePromise<*>::cancel)
        }
        Disposer.register(project, disposable)

        backgroundJob.set(
            nonBlocking<List<Notification>> {
                val librariesToNotify = getLibrariesToNotifyAbout()
                prepareNotifications(librariesToNotify)
            }
                .finishOnUiThread(ModalityState.defaultModalityState()) { notifications ->
                    notifications.forEach {
                        it.notify(project)
                    }
                }
                .expireWith(project) // cancel job when project is disposed
                .coalesceBy(this@KotlinNativeABICompatibilityCheckerService) // cancel previous job when new one is submitted
                .withDocumentsCommitted(project)
                .submit(AppExecutorUtil.getAppExecutorService())
                .onProcessed {
                    backgroundJob.set(null)
                    Disposer.dispose(disposable)
                })
    }

    private fun getLibrariesToNotifyAbout(): Map<String, NativeKlibLibraryInfo> {
        val incompatibleLibraries = getModuleInfosFromIdeaModel(project)
            .filterIsInstance<NativeKlibLibraryInfo>()
            .filter { !it.compatibilityInfo.isCompatible }
            .associateBy { it.libraryRoot }

        val newEntries = if (cachedIncompatibleLibraries.isNotEmpty())
            incompatibleLibraries.filterKeys { it !in cachedIncompatibleLibraries }
        else
            incompatibleLibraries

        cachedIncompatibleLibraries.clear()
        cachedIncompatibleLibraries.addAll(incompatibleLibraries.keys)

        return newEntries
    }

    private fun prepareNotifications(librariesToNotify: Map<String, NativeKlibLibraryInfo>): List<Notification> {
        if (librariesToNotify.isEmpty())
            return emptyList()

        val librariesByGroups = HashMap<Pair<LibraryGroup, Boolean>, MutableList<Pair<String, String>>>()
        librariesToNotify.forEach { (libraryRoot, libraryInfo) ->
            val isOldMetadata = (libraryInfo.compatibilityInfo as? IncompatibleMetadata)?.isOlder ?: true
            val (libraryName, libraryGroup) = parseIDELibraryName(libraryInfo)
            librariesByGroups.computeIfAbsent(libraryGroup to isOldMetadata) { mutableListOf() } += libraryName to libraryRoot
        }

        return librariesByGroups.keys.sortedWith(
            compareBy(
                { (libraryGroup, _) -> libraryGroup },
                { (_, isOldMetadata) -> isOldMetadata }
            )
        ).map { key ->

            val (libraryGroup, isOldMetadata) = key
            val libraries =
                librariesByGroups.getValue(key).sortedWith(compareBy(LIBRARY_NAME_COMPARATOR) { (libraryName, _) -> libraryName })


            val message = when (libraryGroup) {
                is LibraryGroup.FromDistribution -> {
                    val libraryNamesInOneLine = libraries
                        .joinToString(limit = MAX_LIBRARY_NAMES_IN_ONE_LINE) { (libraryName, _) -> libraryName }
                    val text = KotlinGradleNativeBundle.message(
                        "error.incompatible.libraries",
                        libraries.size, libraryGroup.kotlinVersion, libraryNamesInOneLine
                    )
                    val explanation = when (isOldMetadata) {
                        true -> KotlinGradleNativeBundle.message("error.incompatible.libraries.older")
                        false -> KotlinGradleNativeBundle.message("error.incompatible.libraries.newer")
                    }
                    val recipe = KotlinGradleNativeBundle.message("error.incompatible.libraries.recipe", bundledRuntimeVersion())
                    "$text\n\n$explanation\n$recipe"
                }
                is LibraryGroup.ThirdParty -> {
                    val text = when (isOldMetadata) {
                        true -> KotlinGradleNativeBundle.message("error.incompatible.3p.libraries.older", libraries.size)
                        false -> KotlinGradleNativeBundle.message("error.incompatible.3p.libraries.newer", libraries.size)
                    }
                    val librariesLineByLine = libraries.joinToString(separator = "\n") { (libraryName, _) -> libraryName }
                    val recipe = KotlinGradleNativeBundle.message("error.incompatible.3p.libraries.recipe", bundledRuntimeVersion())
                    "$text\n$librariesLineByLine\n\n$recipe"
                }
                is LibraryGroup.User -> {
                    val projectRoot = project.guessProjectDir()?.canonicalPath

                    fun getLibraryTextToPrint(libraryNameAndRoot: Pair<String, String>): String {
                        val (libraryName, libraryRoot) = libraryNameAndRoot

                        val relativeRoot = projectRoot?.let {
                            libraryRoot.substringAfter(projectRoot)
                                .takeIf { it != libraryRoot }
                                ?.trimStart('/', '\\')
                                ?.let { "${'$'}project/$it" }
                        } ?: libraryRoot

                        return KotlinGradleNativeBundle.message("library.name.0.at.1.relative.root", libraryName, relativeRoot)
                    }

                    val text = when (isOldMetadata) {
                        true -> KotlinGradleNativeBundle.message("error.incompatible.user.libraries.older", libraries.size)
                        false -> KotlinGradleNativeBundle.message("error.incompatible.user.libraries.newer", libraries.size)
                    }
                    val librariesLineByLine = libraries.joinToString(separator = "\n", transform = ::getLibraryTextToPrint)
                    val recipe = KotlinGradleNativeBundle.message("error.incompatible.user.libraries.recipe", bundledRuntimeVersion())
                    "$text\n$librariesLineByLine\n\n$recipe"
                }
            }

            Notification(
                NOTIFICATION_GROUP_ID,
                NOTIFICATION_TITLE,
                StringUtilRt.convertLineSeparators(message, "<br/>"),
                NotificationType.ERROR,
                null
            )
        }
    }

    // returns pair of library name and library group
    private fun parseIDELibraryName(libraryInfo: NativeKlibLibraryInfo): Pair<String, LibraryGroup> {
        val ideLibraryName = libraryInfo.library.name?.takeIf(String::isNotEmpty)
        if (ideLibraryName != null) {
            parseIDELibraryName(ideLibraryName)?.let { (kotlinVersion, libraryName) ->
                return libraryName to LibraryGroup.FromDistribution(kotlinVersion)
            }

            if (isGradleLibraryName(ideLibraryName))
                return ideLibraryName to LibraryGroup.ThirdParty
        }

        return (ideLibraryName ?: PathUtilRt.getFileName(libraryInfo.libraryRoot)) to LibraryGroup.User
    }

    companion object {
        private val LIBRARY_NAME_COMPARATOR = Comparator<String> { libraryName1, libraryName2 ->
            when {
                libraryName1 == libraryName2 -> 0
                libraryName1 == KONAN_STDLIB_NAME -> -1 // stdlib must go the first
                libraryName2 == KONAN_STDLIB_NAME -> 1
                else -> libraryName1.compareTo(libraryName2)
            }
        }

        private const val MAX_LIBRARY_NAMES_IN_ONE_LINE = 5

        private val NOTIFICATION_TITLE get() = KotlinGradleNativeBundle.message("error.incompatible.libraries.title")
        private const val NOTIFICATION_GROUP_ID = "Incompatible Kotlin/Native libraries"

        fun getInstance(project: Project): KotlinNativeABICompatibilityCheckerService = project.getServiceSafe()
    }
}

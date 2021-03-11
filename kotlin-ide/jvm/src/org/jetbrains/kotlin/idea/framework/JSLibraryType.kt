/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.ide.JavaUiBundle
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileElement
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.roots.libraries.ui.FileTypeBasedRootFilter
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent
import com.intellij.openapi.roots.libraries.ui.RootDetector
import com.intellij.openapi.roots.ui.configuration.libraryEditor.DefaultLibraryRootsComponentDescriptor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import javax.swing.JComponent

class JSLibraryType : LibraryType<DummyLibraryProperties>(JSLibraryKind) {
    override fun createPropertiesEditor(editorComponent: LibraryEditorComponent<DummyLibraryProperties>) = null

    override fun getCreateActionName() = "Kotlin/JS"

    override fun createNewLibrary(
        parentComponent: JComponent,
        contextDirectory: VirtualFile?,
        project: Project
    ): NewLibraryConfiguration? = LibraryTypeService.getInstance().createLibraryFromFiles(
        RootsComponentDescriptor,
        parentComponent, contextDirectory, this,
        project
    )

    override fun getIcon(properties: DummyLibraryProperties?) = KotlinIcons.JS

    companion object {
        @Suppress("DEPRECATION")
        fun getInstance() = Extensions.findExtension(EP_NAME, JSLibraryType::class.java)
    }

    object RootsComponentDescriptor : DefaultLibraryRootsComponentDescriptor() {
        override fun createAttachFilesChooserDescriptor(libraryName: String?): FileChooserDescriptor {
            val descriptor = FileChooserDescriptor(true, true, true, false, true, true).withFileFilter {
                FileElement.isArchive(it) || isAcceptedForJsLibrary(it.extension)
            }
            descriptor.title = if (StringUtil.isEmpty(libraryName))
                ProjectBundle.message("library.attach.files.action")
            else
                ProjectBundle.message("library.attach.files.to.library.action", libraryName!!)
            descriptor.description = JavaUiBundle.message("library.java.attach.files.description")
            return descriptor
        }

        override fun getRootTypes() = arrayOf(OrderRootType.CLASSES, OrderRootType.SOURCES)

        override fun getRootDetectors(): List<RootDetector> = arrayListOf(
            JSRootFilter,
            FileTypeBasedRootFilter(OrderRootType.SOURCES, false, KotlinFileType.INSTANCE, "sources")
        )
    }

    object JSRootFilter : FileTypeBasedRootFilter(
        OrderRootType.CLASSES, false, PlainTextFileType.INSTANCE,
        KotlinJvmBundle.message("presentable.type.js.files")
    ) {
        override fun isFileAccepted(virtualFile: VirtualFile) = isAcceptedForJsLibrary(virtualFile.extension)

    }
}

private fun isAcceptedForJsLibrary(extension: String?) = extension == "js" || extension == "kjsm"

@Suppress("DEPRECATION_ERROR")
// Can't import a member annotated with DEPRECATION_ERROR
val org.jetbrains.kotlin.config.TargetPlatformKind<*>.libraryKind: PersistentLibraryKind<*>?
    get() = when (this) {
        org.jetbrains.kotlin.config.TargetPlatformKind.JavaScript -> JSLibraryKind
        org.jetbrains.kotlin.config.TargetPlatformKind.Common -> CommonLibraryKind
        else -> null
    }

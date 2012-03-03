package org.jetbrains.kotlin.doc.templates

import kotlin.*
import org.jetbrains.kotlin.template.*
import kotlin.io.*
import kotlin.util.*
import java.util.*
import org.jetbrains.kotlin.model.KModel

class PackageListTemplate(val model: KModel) : KDocTemplate() {
  override fun render() {
      for (p in model.packages) {
          println(p.name)
      }
  }
}

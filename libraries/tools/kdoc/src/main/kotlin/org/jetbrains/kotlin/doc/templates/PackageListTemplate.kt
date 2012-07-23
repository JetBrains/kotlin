package org.jetbrains.kotlin.doc.templates

import org.jetbrains.kotlin.doc.model.KModel

class PackageListTemplate(val model: KModel) : KDocTemplate() {
  override fun render() {
      for (p in model.packages) {
          println(p.name)
      }
  }
}

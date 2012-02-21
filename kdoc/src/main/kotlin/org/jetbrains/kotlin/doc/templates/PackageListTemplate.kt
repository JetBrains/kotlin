package org.jetbrains.kotlin.doc.templates

import std.*
import org.jetbrains.kotlin.template.*
import std.io.*
import std.util.*
import java.util.*
import org.jetbrains.kotlin.model.KModel

class PackageListTemplate(val model: KModel) : TextTemplate() {
  override fun render() {
      for (p in model.packages) {
          println(p.name)
      }
  }
}

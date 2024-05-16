package hprof

import kdump.literal
import text.Pretty
import text.binary
import text.decimal
import text.hexadecimal
import text.name

fun Pretty.item(profile: Profile) {
  struct("profile") {
    field("id size") { name(profile.idSize) }
    field("time") { decimal(profile.time) }
    struct("records") { profile.records.forEach { item(it) } }
  }
}

fun Pretty.item(record: Profile.Record) {
  when (record) {
    is HeapDump ->
      struct("heap dump") {
        record.records.forEach { item(it) }
      }

    HeapDumpEnd ->
      struct("head dump end") {}

    is HeapDumpSection ->
      struct("heap dump section") {
        record.records.forEach { item(it) }
      }

    is LoadClass ->
      struct("load class") {
        field("class serial number") { decimal(record.classSerialNumber) }
        field("class object id") { hexadecimal(record.classObjectId) }
        field("stack trace serial number") { decimal(record.stackTraceSerialNumber) }
        field("class name string id") { hexadecimal(record.classNameStringId) }
      }

    is StackFrame ->
      struct("stack frame") {
        field("stack frame id") { hexadecimal(record.stackFrameId) }
        field("method name string id") { hexadecimal(record.methodNameStringId) }
        field("method signature string id") { hexadecimal(record.methodSignatureStringId) }
        field("source filename string id") { hexadecimal(record.sourceFileNameStringId) }
        field("class serial number") { decimal(record.classSerialNumber) }
        field("line number") { decimal(record.lineNumber) }
      }

    is StackTrace ->
      struct("stack trace") {
        field("serial number") { decimal(record.serialNumber) }
        field("thread serial number") { decimal(record.threadSerialNumber) }
        struct("stack frame ids") { record.stackFrameIds.forEach { hexadecimal(it) } }
      }

    is StartThread ->
      struct("start thread") {
        field("thread serial number") { decimal(record.threadSerialNumber) }
        field("thread object id") { hexadecimal(record.threadObjectId) }
        field("thread name string id") { hexadecimal(record.threadNameStringId) }
        field("thread group name id") { hexadecimal(record.threadGroupNameId) }
        field("thread parent group name id ") { hexadecimal(record.threadParentGroupNameId) }
      }

    is StringConstant ->
      struct("string constant") {
        field("id") { hexadecimal(record.id) }
        field("string") { literal(record.string) }
      }

    is UnknownRecord ->
      struct("unknown record") {
        field("tag") { hexadecimal(record.tag) }
        field("byte array") { binary(record.byteArray) }
      }
  }
}

fun Pretty.item(record: HeapDump.Record) {
  when (record) {
    is ClassDump ->
      struct("class dump") {
        field("class object id") { hexadecimal(record.classObjectId) }
        field("stack trace serial number") { decimal(record.stackTraceSerialNumber) }
        field("super class object id") { hexadecimal(record.superClassObjectId) }
        field("class loader object id") { hexadecimal(record.classLoaderObjectId) }
        field("signers object id") { hexadecimal(record.signersObjectId) }
        field("protection domain object id") { hexadecimal(record.protectionDomainObjectId) }
        field("reserved id 1") { hexadecimal(record.reservedId1) }
        field("reserved id 2") { hexadecimal(record.reservedId2) }
        field("instance size") { decimal(record.instanceSize) }
        struct("constants") { record.constants.forEach { item(it) } }
        struct("static fields") { record.staticFields.forEach { item(it) } }
        struct("instance fields") { record.instanceFields.forEach { item(it) } }
      }

    is InstanceDump ->
      struct("instance dump") {
        field("object id") { hexadecimal(record.objectId) }
        field("stack trace serial number") { decimal(record.stackTraceSerialNumber) }
        field("class object id") { hexadecimal(record.classObjectId) }
        field("byte array") { binary(record.byteArray) }
      }

    is ObjectArrayDump ->
      struct("object array dump") {
        field("array object id") { hexadecimal(record.arrayObjectId) }
        field("stack frame serial number") { decimal(record.stackTraceSerialNumber) }
        field("number of elements") { decimal(record.numberOfElements) }
        field("array class object id") { hexadecimal(record.arrayClassObjectId) }
        field("byte array") { binary(record.byteArray) }
      }

    is PrimitiveArrayDump ->
      struct("primitive array dump") {
        field("array object id") { hexadecimal(record.arrayObjectId) }
        field("stack frame serial number") { decimal(record.stackTraceSerialNumber) }
        field("number of elements") { decimal(record.numberOfElements) }
        field("array element type") { name(record.arrayElementType) }
        field("byte array") { binary(record.byteArray) }
      }

    is RootJavaFrame ->
      struct("root java frame") {
        field("object id") { hexadecimal(record.objectId) }
        field("thread serial number") { decimal(record.threadSerialNumber) }
        field("frame number") { decimal(record.frameNumber) }
      }

    is RootJniGlobal ->
      struct("root jni global") {
        field("object id") { hexadecimal(record.objectId) }
        field("ref id") { decimal(record.refId) }
      }

    is RootJniLocal ->
      struct("root jni local") {
        field("object id") { hexadecimal(record.objectId) }
        field("thread serial number") { decimal(record.threadSerialNumber) }
        field("thread frame number") { decimal(record.threadFrameNumber) }
      }

    is RootStickyClass ->
      struct("root sticky class") {
        field("object id") { hexadecimal(record.objectId) }
      }

    is RootThreadObject ->
      struct("root thread object") {
        field("thread object id") { hexadecimal(record.threadObjectId) }
        field("thread serial number") { decimal(record.threadSerialNumber) }
        field("stack trace serial number") { decimal(record.stackTraceSerialNumber) }
      }

    is RootUnknown ->
      struct("root unknown") {
        field("object id") { hexadecimal(record.objectId) }
      }
  }
}

fun Pretty.item(constant: Constant) {
  struct("constant") {
    field("index") { decimal(constant.index.toInt()) }
    field("type") { name(constant.type) }
    field("byte array") { binary(constant.byteArray) }
  }
}

fun Pretty.item(staticField: StaticField) {
  struct("static field") {
    field("name string id") { hexadecimal(staticField.nameStringId) }
    field("type") { name(staticField.type) }
    field("byte array") { binary(staticField.byteArray) }
  }
}

fun Pretty.item(instanceField: InstanceField) {
  struct("instance field") {
    field("name string id") { hexadecimal(instanceField.nameStringId) }
    field("type") { name(instanceField.type) }
  }
}

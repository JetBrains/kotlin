//
// Created by user on 7/12/16.
//

#include "kotlin_field_generator.h"
#include <vector>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/descriptor.h>
#include <iostream>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

/**
 * Gets equivalent Kotlin-type for a given field descriptor.
 * This method takes protobuf field type into account, adding 'List<>'
 * for repeated fields, and '?' for optional.
 */
string FieldGenerator::protobufToKotlinField() const {
    string  preamble = "",
            postamble = "";
    switch (modifier) {
        case FieldDescriptor::LABEL_REQUIRED:
            break;
        case FieldDescriptor::LABEL_OPTIONAL:
            postamble = "?";
            break;
        case FieldDescriptor::LABEL_REPEATED:
            preamble  = "List <";
            postamble = "> ";
            break;
    }
    return preamble + protobufToKotlinType() + postamble;
}
/**
 * Simply maps protobuf field type to corresponding Kotlin.
 */
string FieldGenerator::protobufToKotlinType() const {
    FieldDescriptor::Type type = descriptor->type();
    switch(type) {
        case FieldDescriptor::TYPE_BOOL:
            return "Boolean";
        case FieldDescriptor::TYPE_BYTES:
            return "";  // TODO: support bytes type
        case FieldDescriptor::TYPE_DOUBLE:
            return "Double";
        case FieldDescriptor::TYPE_ENUM:
            return string(descriptor->enum_type()->name());
        case FieldDescriptor::TYPE_FIXED32:
            // we map uint32 into Int, storing top bit in sign bit
            return "Int";
        case FieldDescriptor::TYPE_FIXED64:
            // we map uint64 into Long, storing top bit in sign bit
            return "Long";
        case FieldDescriptor::TYPE_FLOAT:
            return "Float";
        case FieldDescriptor::TYPE_GROUP:
            return "";     // @deprecated //TODO: make proper error handling there
        case FieldDescriptor::TYPE_INT32:
            return "Int";
        case FieldDescriptor::TYPE_INT64:
            return "Long";
        case FieldDescriptor::TYPE_MESSAGE:
            return string(descriptor->message_type()->name());
        case FieldDescriptor::TYPE_SFIXED32:
            return "Int";
        case FieldDescriptor::TYPE_SFIXED64:
            return "Long";
        case FieldDescriptor::TYPE_SINT32:
            return "Int";
        case FieldDescriptor::TYPE_SINT64:
            return "Long";
        case FieldDescriptor::TYPE_STRING:
            return "kotlin.String";
        case FieldDescriptor::TYPE_UINT32:
            return "Int";            // see notes for TYPE_FIXED32
        case FieldDescriptor::TYPE_UINT64:
            return "Long";           // see notes for TYPE_FIXED64
    }
}

string FieldGenerator::getInitValue() const {
    if (descriptor->is_optional()) {
        return "null";
    }
    if (descriptor->is_repeated())
        return "listOf()";
    return fieldName + "()";
}

void FieldGenerator::generateCode(io::Printer *printer) const {
    /** Generate Kotlin-code for field.
     *  Note that we use 'val' everywhere, as we want messages to be immutable.
     *  For constructing Messages corresponding Builders should be used.
     */
    map<string, string> vars;
    vars["name"] = simpleName;
    vars["field"] = protobufToKotlinField();
    vars["initValue"] = initValue;
    printer->Print(vars, "val $name$ : $field$ = $initValue$");
}

FieldGenerator::FieldGenerator(FieldDescriptor const * descriptor)
        : descriptor(descriptor)
        , modifier(descriptor->label())
        , simpleName(descriptor->name())
        , fieldName(protobufToKotlinField())
        , initValue(getInitValue())
{ }

} // namespace kotlin
} // namspace compiler
} // namespace protobuf
} // namespace google
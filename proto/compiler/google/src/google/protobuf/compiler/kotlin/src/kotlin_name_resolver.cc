//
// Created by user on 7/14/16.
//

#include "kotlin_name_resolver.h"
#include <google/protobuf/descriptor.h>

#include <string>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

namespace name_resolving {

using namespace std;

string makeFirstLetterUpper(std::string s) {
    s[0] = std::toupper(s[0]);
    return s;
}

string getFileNameWithoutExtension(string fullName) {
    size_t file_extension_index = fullName.find_last_of(".");
    return fullName.substr(0, file_extension_index);
}

string getKotlinOutputByProtoName(string protoName) {
    string justName = getFileNameWithoutExtension(protoName);
    return justName + ".kt";
}

string protobufToKotlinType(FieldDescriptor const * descriptor) {
    FieldDescriptor::Type type = descriptor->type();
    switch(type) {
        case FieldDescriptor::TYPE_BOOL:
            return "Boolean";
        case FieldDescriptor::TYPE_BYTES:
            return "ByteArray";
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

string protobufToKotlinField(FieldDescriptor const * descriptor) {
    FieldDescriptor::Label modifier = descriptor->label();
    string  preamble = "",
            postamble = "";
    switch (modifier) {
        case FieldDescriptor::LABEL_REQUIRED:
            break;
        case FieldDescriptor::LABEL_OPTIONAL:
            break;
        case FieldDescriptor::LABEL_REPEATED:
#ifndef KOTLIN_GENERATED_CODE_LANGUAGE_LEVEL_LOW
            preamble = "MutableList <";
            postamble = "> ";
            break;
#else
        preamble  = "Array <";
            postamble = "> ";
            break;
#endif
    }
    return preamble + protobufToKotlinType(descriptor) + postamble;
}

string protobufTypeToInitValue(FieldDescriptor const * descriptor) {
    FieldDescriptor::Type type = descriptor->type();
    switch(type) {
        case FieldDescriptor::TYPE_BOOL:
            return "false";
        case FieldDescriptor::TYPE_BYTES:
            return "ByteArray(0)";
        case FieldDescriptor::TYPE_DOUBLE:
            return "0.0";
        case FieldDescriptor::TYPE_ENUM: {
            string enumType = descriptor->enum_type()->name();
            return enumType + ".fromIntTo" + enumType + "(0)";   // produce enum from 0, as demanded by Google
        }
        case FieldDescriptor::TYPE_FIXED32:
            return "0";
        case FieldDescriptor::TYPE_FIXED64:
            return "0L";
        case FieldDescriptor::TYPE_FLOAT:
            return "0f";
        case FieldDescriptor::TYPE_INT32:
            return "0";
        case FieldDescriptor::TYPE_INT64:
            return "0L";
        case FieldDescriptor::TYPE_MESSAGE:
            return string(descriptor->message_type()->name()) + "()";
        case FieldDescriptor::TYPE_SFIXED32:
            return "0";
        case FieldDescriptor::TYPE_SFIXED64:
            return "0L";
        case FieldDescriptor::TYPE_SINT32:
            return "0";
        case FieldDescriptor::TYPE_SINT64:
            return "0L";
        case FieldDescriptor::TYPE_STRING:
            return "\"\"";
        case FieldDescriptor::TYPE_UINT32:
            return "0";            // see notes for TYPE_FIXED32
        case FieldDescriptor::TYPE_UINT64:
            return "0L";           // see notes for TYPE_FIXED64
    }
}

string protobufTypeToKotlinFunctionSuffix(FieldDescriptor::Type type)  {
    switch (type) {
        case FieldDescriptor::TYPE_DOUBLE:
            return "Double";
        case FieldDescriptor::TYPE_FLOAT:
            return "Float";
        case FieldDescriptor::TYPE_INT64:
            return "Int64";
        case FieldDescriptor::TYPE_UINT64:
            return "UInt64";
        case FieldDescriptor::TYPE_INT32:
            return "Int32";
        case FieldDescriptor::TYPE_FIXED64:
            return "Fixed64";
        case FieldDescriptor::TYPE_FIXED32:
            return "Fixed32";
        case FieldDescriptor::TYPE_BOOL:
            return "Bool";
        case FieldDescriptor::TYPE_STRING:
            return "String";
        case FieldDescriptor::TYPE_MESSAGE:
            return "Message";
        case FieldDescriptor::TYPE_BYTES:
            return "Bytes";
        case FieldDescriptor::TYPE_UINT32:
            return "UInt32";
        case FieldDescriptor::TYPE_ENUM:
            return "Enum";
        case FieldDescriptor::TYPE_SFIXED32:
            return "SFixed32";
        case FieldDescriptor::TYPE_SFIXED64:
            return "SFixed64";
        case FieldDescriptor::TYPE_SINT32:
            return "SInt32";
        case FieldDescriptor::TYPE_SINT64:
            return "SInt64";
    }
}
} // namespace name_resolving
} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google
//
// Created by user on 7/14/16.
//

#include "kotlin_name_resolver.h"
#include "kotlin_field_generator.h"
#include <google/protobuf/descriptor.h>
#include "UnreachableStateException.h"
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
    string fileWithoutPath = protoName.substr(protoName.find_last_of("/") + 1, protoName.length());
    string justName = getFileNameWithoutExtension(fileWithoutPath);
    return justName + ".kt";
}

string protobufToKotlinType(FieldDescriptor::Type type) {
    switch(type) {
        case FieldDescriptor::TYPE_BOOL:
            return "Boolean";
        case FieldDescriptor::TYPE_BYTES:
            return "ByteArray";
        case FieldDescriptor::TYPE_DOUBLE:
            return "Double";
        case FieldDescriptor::TYPE_ENUM:
            throw UnreachableStateException
                    ("Error: mapping protobuf enum types to kotlin types should be resolved by field generator, not by protobufToKotlinType function");
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
            throw UnreachableStateException
                    ("Error: mapping protobuf message types to kotlin types should be resolved by field generator, not by protobufToKotlinType function");
        case FieldDescriptor::TYPE_SFIXED32:
            return "Int";
        case FieldDescriptor::TYPE_SFIXED64:
            return "Long";
        case FieldDescriptor::TYPE_SINT32:
            return "Int";
        case FieldDescriptor::TYPE_SINT64:
            return "Long";
        case FieldDescriptor::TYPE_STRING:
            return "String";
        case FieldDescriptor::TYPE_UINT32:
            return "Int";            // see notes for TYPE_FIXED32
        case FieldDescriptor::TYPE_UINT64:
            return "Long";           // see notes for TYPE_FIXED64
    }
}


// TODO: think about nested arrays
string protobufTypeToInitValue(FieldDescriptor::Type type) {
    switch(type) {
        case FieldDescriptor::TYPE_BOOL:
            return "false";
        case FieldDescriptor::TYPE_BYTES:
            return "ByteArray(0)";
        case FieldDescriptor::TYPE_DOUBLE:
            return "0.0";
        case FieldDescriptor::TYPE_ENUM:
            throw UnreachableStateException("Error: getting init values of enums should be handled by FieldGenerator, not by protobufToInitValue");
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
            throw UnreachableStateException("Error: getting init values of enums should be handled by FieldGenerator, not by protobufToInitValue");
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

string protobufTypeToKotlinWireType(FieldDescriptor::Type type) {
    switch (type) {
        case FieldDescriptor::TYPE_DOUBLE:
            return "WireType.FIX_64";
        case FieldDescriptor::TYPE_FLOAT:
            return "WireType.FIX_32";
        case FieldDescriptor::TYPE_INT64:
            return "WireType.VARINT";
        case FieldDescriptor::TYPE_UINT64:
            return "WireType.VARINT";
        case FieldDescriptor::TYPE_INT32:
            return "WireType.VARINT";
        case FieldDescriptor::TYPE_FIXED64:
            return "WireType.FIX_64";
        case FieldDescriptor::TYPE_FIXED32:
            return "WireType.FIX_32";
        case FieldDescriptor::TYPE_BOOL:
            return "WireType.VARINT";
        case FieldDescriptor::TYPE_STRING:
            return "WireType.LENGTH_DELIMITED";
        case FieldDescriptor::TYPE_MESSAGE:
            return "WireType.LENGTH_DELIMITED";
        case FieldDescriptor::TYPE_BYTES:
            return "WireType.LENGTH_DELIMITED";
        case FieldDescriptor::TYPE_UINT32:
            return "WireType.VARINT";
        case FieldDescriptor::TYPE_ENUM:
            return "WireType.VARINT";
        case FieldDescriptor::TYPE_SFIXED32:
            return "WireType.FIX_32";
        case FieldDescriptor::TYPE_SFIXED64:
            return "WireType.FIX_64";
        case FieldDescriptor::TYPE_SINT32:
            return "WireType.VARINT";
        case FieldDescriptor::TYPE_SINT64:
            return "WireType.VARINT";
    }
}
} // namespace name_resolving

NameResolver::NameResolver() {
    names = map<string, string>();
    builders = map<string, string>();
    generators = map<string, ClassGenerator *>();
}

void NameResolver::addClass(string simpleName, string parentName) {
    if (parentName == "") {
        names[simpleName] = simpleName;
        builders[simpleName] = simpleName + ".Builder" + simpleName;
    }
    else
    {
        names[simpleName] = parentName + "." + simpleName;
        builders[simpleName] = parentName + "." + simpleName + ".Builder" + simpleName;
    }
}

void NameResolver::addGeneratorForClass(string simpleName, ClassGenerator *classGenerator) {
    generators[simpleName] = classGenerator;
}
string NameResolver::getClassName(string simpleName) {
    return names[simpleName];
}

string NameResolver::getBuilderName(string classSimpleName) {
    return builders[classSimpleName];
}

ClassGenerator * NameResolver::getClassGenerator(string simpleName) {
    return generators[simpleName];
}

} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google


//
// Created by user on 7/12/16.
//

#ifndef PROTOBUF_KOTLIN_ENUM_GENERATOR_H
#define PROTOBUF_KOTLIN_ENUM_GENERATOR_H

#include <google/protobuf/io/printer.h>
#include <google/protobuf/descriptor.h>
#include "kotlin_name_resolver.h"

class NameResolver;     // declared in "kotlin_name_resolver.h"

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

class EnumValueGenerator {
public:
    EnumValueGenerator(EnumValueDescriptor const * descriptor);
    string simpleName;
    int ordinal;

    void generateCode(io::Printer *) const;
};

class EnumGenerator {
public:
    EnumGenerator(EnumDescriptor const * descriptor, NameResolver * nameResolver);
    ~EnumGenerator();
    string simpleName;
    vector <EnumValueGenerator *> enumValues;

    /* Return full-qualified name of enum */
    string getFullType() const;

    void generateCode(io::Printer *) const;

private:
    void generateEnumConverter(io::Printer *printer) const;
    NameResolver * nameResolver;
};

} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google


#endif //PROTOBUF_KOTLIN_ENUM_GENERATOR_H


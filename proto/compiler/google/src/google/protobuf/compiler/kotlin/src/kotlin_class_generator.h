//
// Created by user on 7/11/16.
//

#ifndef SRC_KOTLIN_CLASS_GENERATOR_H
#define SRC_KOTLIN_CLASS_GENERATOR_H

#include <vector>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/descriptor.h>
#include "kotlin_field_generator.h"
#include "kotlin_enum_generator.h"
#include "kotlin_name_resolver.h"

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

class FieldGenerator;   // declared in "kotlin_file_generator.h"
class NameResolver;     // declared in "kotlin_name_resolver.h"
class EnumGenerator;    // declared in "kotlin_enum_generator.h"

class ClassGenerator { // TODO ProtoClass
public:
    string getSimpleType() const;
    string getFullType() const;
    string getBuidlerSimpleType() const;
    string getBuilderFullType() const;
    string getBuilderInitValue() const;

    vector <FieldGenerator *>   properties;
    vector <ClassGenerator *>   classesDeclarations; // TODO nestedClasses
    vector <EnumGenerator  *>   enumsDeclaraions; // TODO ProtoEnum
    ClassGenerator          (Descriptor const * descriptor, NameResolver * nameResolver);
    ~ClassGenerator         ();

	// KtClassGenerator(ProtoClass)::generate()
    void generateCode (io::Printer * printer, bool isBuilder = false) const;
private:
    /**
     * Flag isBuilder used for reducing code repeating, as code for class itself
     * and for its inner builder are structurally very alike and can be generated
     * with very little differences (like changing 'val's to 'var's and etc.)
     */
    Descriptor const * descriptor;
    NameResolver * nameResolver;

    void generateHeader         (io::Printer * printer, bool isBuilder = false) const;
    void generateBuilder        (io::Printer * printer) const;
    void generateBuildMethod    (io::Printer * printer) const;
    void generateInitSection    (io::Printer * printer) const;
    void generateWriteToMethod  (io::Printer * printer) const;
    void generateMergeMethods   (io::Printer * printer) const;
    void generateParseMethods   (io::Printer * printer) const;
    void generateGetSizeMethod  (io::Printer * printer) const;
};

} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google
#endif //SRC_KOTLIN_CLASS_GENERATOR_H

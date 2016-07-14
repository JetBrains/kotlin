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

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

// wrapper for enum CLASS/INTERFACE with convenience method of getting name
class ClassModifier {
public:
    enum Type {
        CLASS,
        INTERFACE
    };
    ClassModifier();
    ClassModifier(Type type);
    Type type;

    string const getName() const;
};

class ClassGenerator {
public:
    string                      simpleName;
    vector <FieldGenerator *>   properties;
    vector <ClassGenerator *>   classesDeclarations;
    vector <EnumGenerator  *>   enumsDeclaraions;

    ClassGenerator          (Descriptor const * descriptor);
    ~ClassGenerator         ();


    void generateCode (io::Printer * printer, bool isBuilder = false) const;
private:
    Descriptor const * descriptor;
    void generateBuilder (io::Printer * printer) const;
    void generateBuildMethod (io::Printer * printer) const;
    void generateInitSection (io::Printer * printer) const;
    void generateSetters (io::Printer * printer) const;
    /**
     * Flag isBuilder used for reducing code repeating, as code for class itself
     * and for its inner builder are structurally very alike and can be generated
     * with very little differences (like changing 'val's to 'var's and etc.)
     */
    void generateHeader(io::Printer * printer, bool isBuilder = false) const;

    /**
     * IsRead flag indicates that readFrom method should be generated, otherwise
     * writeTo method is generated. Motivation is similar to the isBuilder flag:
     * both methods are structurally the same with some trivial substitutions
     * (read -> write and etc.)
     */
    void generateSerializersNoTag(io::Printer *printer, bool isRead = false) const;
    void generateSerializers(io::Printer * printer, bool isRead = false) const;
    void generateMergeFrom(io::Printer * printer) const;
};

} // namespace kotlin
} // namspace compiler
} // namespace protobuf
} // namespace google
#endif //SRC_KOTLIN_CLASS_GENERATOR_H

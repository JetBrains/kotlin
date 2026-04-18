import os
import argparse

# Default list of tests provided by the user
DEFAULT_TESTS = [
    "javaRawTypesAndGenericsErasure",
    "staticOverrideOnKJJ",
    "lateinitPropertiesSeparateModule",
    "intersectionWithPublishedApiOverride",
    "allPropertiesAndMethodsWithSeparateModuleKJJ",
    "allPropertiesAndMethodsKJ",
    "intersectionWithSeparateModule",
    "fakeOverrideOfRaw",
    "basicNullabilityAnnotationOverride",
    "intersectionNullabilityAnnotation",
    "typeParameterAnnotationOverride",
    "intersectionWithGenericOverride",
    "intersectionWithExplicitOverride",
    "basicOverride",
    "intersectionOverride",
    "substitutionOverride",
    "arraysFromBuiltins",
    "genericClassInDifferentModule",
    "nullCheckOnInterfaceDelegation",
    "enhancedNullabilityInCatch",
    "enhancedNullability",
    "stringVsT",
    "nnStringVsTAny",
    "stringVsTAny",
    "nnStringVsTXArray",
    "nnStringVsTConstrained",
    "nnStringVsT",
    "stringVsTXArray",
    "stringVsTXString",
    "nnStringVsTXString",
    "stringVsTConstrained",
    "stringVsAny",
    "nullCheckInElvisRhs",
    "definitelyNonNullWithJava",
    "delegatedImplementationOfJavaInterface",
    "kt43217",
    "classLiteralInAnnotation",
    "annotationRetentionsMultiModule",
    "specialAnnotationsMetadata",
    "javaRecordComponentAccess",
    "firBuilder",
    "internalPotentialFakeOverride",
    "internalPotentialOverride",
    "signatureComputationComplexJavaGeneric",
    "whenWithSubjectVariable",
    "ifWithLoop",
    "fieldAccess_regular",
    "nullCheckOnGenericLambdaReturn",
    "simpleOperators",
    "fieldAccess_generic",
    "nullCheckOnLambdaReturn",
    "kt47245",
    "withVarargViewedAsArray",
    "protectedJavaFieldRef",
    "fieldAccess_invisible",
    "breakContinueInWhen",
    "coercionInLoop",
]

root_dir = os.path.join(os.getcwd(), "compiler/testData/ir/irText")

def find_files(target_names):
    found = {}
    for dirpath, dirnames, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith('.kt'):
                base = filename[:-3]
                if base in target_names:
                    found[base] = os.path.join(dirpath, filename)
    return found

def main():
    parser = argparse.ArgumentParser(description="Mute or unmute a set of JKlib tests.")
    parser.add_argument("--action", choices=["mute", "unmute"], required=True, help="Action to perform")
    parser.add_argument("--tests", nargs="*", help="Specific test names to process (optional, defaults to the hardcoded list)")
    
    args = parser.parse_args()
    
    test_list = args.tests if args.tests else DEFAULT_TESTS
    test_set = set(test_list)
    
    print(f"Action: {args.action.upper()}")
    print(f"Processing {len(test_set)} unique test names.")
    
    found_files = find_files(test_set)
    print(f"Found {len(found_files)} matching files in directory.")
    
    directive = "// IGNORE_BACKEND: JKLIB"
    
    updated_count = 0
    for name in test_list:
        if name not in found_files:
            print(f"Skipped {name} (File not found)")
            continue
            
        path = found_files[name]
        with open(path, 'r') as f:
            content = f.read()
            
        if args.action == "mute":
            if directive not in content:
                # Add at the top
                new_content = directive + "\n" + content
                with open(path, 'w') as f:
                    f.write(new_content)
                print(f"Muted: {name}")
                updated_count += 1
            else:
                print(f"Already muted: {name}")
                
        elif args.action == "unmute":
            if directive in content:
                # Remove the directive (and the newline after it if present)
                new_content = content.replace(directive + "\n", "")
                # Fallback if it didn't have a newline or was at the end
                if directive in new_content:
                    new_content = new_content.replace(directive, "")
                    
                with open(path, 'w') as f:
                    f.write(new_content)
                print(f"Unmuted: {name}")
                updated_count += 1
            else:
                print(f"Already unmuted: {name}")
                
    print(f"\nDone. Total files updated: {updated_count}")

if __name__ == "__main__":
    main()

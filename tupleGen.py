def generateTupleClass(i):
    f = open("stdlib/src/jet/Tuple" + str(i) + ".java", "w")
    f.write("package jet;\n\n")
    f.write("public class Tuple" + str(i) + "<")
    f.write(", ".join(("T" + str(j)) for j in range(1, i+1)))
    f.write("> {\n")
    for j in range(1, i+1):
        f.write("    public final T{0} _{0};\n".format(j))

    f.write("\n    public Tuple" + str(i) + "(")
    f.write(", ".join("T{0} t{0}".format(j) for j in range(1, i+1)))
    f.write(") {\n")
    for j in range(1, i+1):
        f.write("        _{0} = t{0};\n".format(j))
    f.write("    }\n\n")

    f.write("    @Override\n    public String toString() {\n")
    f.write('        return "(" + ' + ' + ", " + '.join("_{0}".format(j) for j in range(1, i+1)) + ' + ")";\n')
    f.write("    }\n")

    f.write("    @Override\n    public boolean equals(Object o) {\n")
    f.write("        if (this == o) return true;\n")
    f.write("        if (o == null || getClass() != o.getClass()) return false;\n\n")
    f.write("        Tuple{0} t = (Tuple{0}) o;\n".format(i))
    for j in range(1, i+1):
        f.write("        if (_{0} != null ? !_{0}.equals(t._{0}) : t._{0} != null) return false;\n".format(j))
    f.write("        return true;\n")
    f.write("    }\n")

    f.write("    @Override\n    public int hashCode() {\n")
    f.write("        int result = _1 != null ? _1.hashCode() : 0;\n")
    for j in range(2, i+1):
        f.write("        result = 31 * result + (_{0} != null ? _{0}.hashCode() : 0);\n".format(j))
    f.write("        return result;\n")
    f.write("    }\n")

    f.write("}")
    f.close()

for i in range(1, 23):
    generateTupleClass(i)

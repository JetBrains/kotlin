import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val csv = """
pclass;survived;name;sex;age;sibsp;parch;ticket;fare;cabin;embarked;boat;body;homedest
1;1;Allen, Miss. Elisabeth;female;29;0;0;24160;211.34;B5;S;2;;St Louis, MO
1;1;Anderson, Mr. Harry;male;48;0;0;19952;26.55;E12;S;3;;New York, NY
1;0;Andrews, Mr. Thomas;male;39;0;0;112050;0;A36;S;;;Belfast, NI
1;1;Appleton, Mrs. Charlotte;female;53;2;0;11769;51.48;C101;S;D;;Bayside, Queens, NY
1;0;Astor, Col. John Jacob;male;47;1;0;PC17757;227.53;C62;C;;124;New York, NY
1;1;Astor, Mrs. Madeleine;female;18;1;0;PC17757;227.53;C62;C;4;;New York, NY
1;1;Behr, Mr. Karl;male;26;0;0;111369;30;C148;C;5;;New York, NY
1;0;Blackwell, Mr. Stephen;male;45;0;0;113784;35.5;T;S;;;Trenton, NJ
1;1;Bonnell, Miss. Caroline;female;30;0;0;36928;164.87;C7;S;8;;Youngstown, OH
1;1;Smith, Miss. Jane;female;15;0;0;12345;50;B10;S;2;;New York, NY
""".trim()

    val df = DataFrame.readCsvStr(csv, delimiter = ';')
        // Complete schema is not necessary,
        // a few string api operations can provide enough info for further type safe data manipulations
        .convert { "survived"<Int>() }.with { it == 1 }
        .convert("age").toInt()

    // filter
    df.filter {
        survived && "homedest"<String?>().orEmpty().endsWith("NY") && (age?.let { it in 10..20 } ?: false)
    }.let {
        val v: Boolean = it[0].survived
        it.compareSchemas()
    }

    // add column
    df.add("birthYear") { age?.let { 1912 - it } }.let {
        val v: Int? = it[0].birthYear
        it.compareSchemas()
    }

    // sort rows
    df.sortByDesc { age }.let {
        val v: Int? = it[0].age
        it.compareSchemas()
    }

    // aggregate data
    df.groupBy { "pclass"() }.aggregate {
        maxBy { age }["name"] into "oldest person"
        count { survived } into "survived"
    }.let {
        val v: Any? = it[0].`oldest person`
        it.compareSchemas()
    }

    return "OK"
}
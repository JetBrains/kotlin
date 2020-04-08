// EXPECTED_DUPLICATED_HIGHLIGHTING

fun <info descr="null">bar</info>(<info descr="null">block</info>: () -> <info descr="null">Int</info>) = <info descr="null"><info descr="null">block</info></info>()

fun <info descr="null">foo</info>(): <info descr="null">Int</info> {
    <info descr="null"><info descr="null">bar</info></info> <info descr="null" textAttributesKey="KOTLIN_LABEL">label@</info> {
        return<info descr="null" textAttributesKey="KOTLIN_LABEL">@label</info> 2
    }

    <info descr="null" textAttributesKey="KOTLIN_LABEL">loop@</info> for (<info descr="null">i</info> in 1..100) {
        break<info descr="null" textAttributesKey="KOTLIN_LABEL">@loop</info>
    }

    <info descr="null" textAttributesKey="KOTLIN_LABEL">loop2@</info> for (<info descr="null">i</info> in 1..100) {
        break<error descr="There should be no space or comments before '@' in label reference"> </error><info descr="null" textAttributesKey="KOTLIN_LABEL">@loop2</info>
    }

    return 1
}
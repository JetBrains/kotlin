fun test(irrelevantClass: IrrelevantClass) {
    copy() // expect: ok, actual: DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING
    irrelevantClass.copy() // expect: ok, actual: DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING
}

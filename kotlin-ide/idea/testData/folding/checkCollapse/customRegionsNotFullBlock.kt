val action2 = <fold text='{...}' expand='true'>{
    <fold text='start' expand='true'>//<editor-fold desc="start">
    foo()
    //</editor-fold"></fold>

    foo()

    <fold text='middle' expand='true'>//<editor-fold desc="middle">
    foo()
    //</editor-fold"></fold>

    foo()

    // At the end
    <fold text='end' expand='true'>//<editor-fold desc="end">
    foo()
    //</editor-fold"></fold>
}</fold>

fun foo() {}


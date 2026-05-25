import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@Composable fun Test01(p0: Int, p1: Int, p2: Int, p3: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test02(p0: Int, p1: Int, p3: Int, p2: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test03(p0: Int, p2: Int, p1: Int, p3: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test04(p0: Int, p2: Int, p3: Int, p1: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test05(p0: Int, p3: Int, p1: Int, p2: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test06(p0: Int, p3: Int, p2: Int, p1: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test07(p1: Int, p0: Int, p2: Int, p3: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test08(p1: Int, p0: Int, p3: Int, p2: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test09(p1: Int, p2: Int, p0: Int, p3: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test00(p1: Int, p2: Int, p3: Int, p0: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test11(p1: Int, p3: Int, p0: Int, p2: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test12(p1: Int, p3: Int, p2: Int, p0: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test13(p2: Int, p0: Int, p1: Int, p3: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test14(p2: Int, p0: Int, p3: Int, p1: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test15(p2: Int, p1: Int, p0: Int, p3: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test16(p2: Int, p1: Int, p3: Int, p0: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test17(p2: Int, p3: Int, p0: Int, p1: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test18(p2: Int, p3: Int, p1: Int, p0: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test19(p3: Int, p0: Int, p1: Int, p2: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test20(p3: Int, p0: Int, p2: Int, p1: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test21(p3: Int, p1: Int, p0: Int, p2: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test22(p3: Int, p1: Int, p2: Int, p0: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test23(p3: Int, p2: Int, p0: Int, p1: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}
@Composable fun Test24(p3: Int, p2: Int, p1: Int, p0: Int) {
    used(p0)
    used(p1)
    used(p2)
    used(p3)
}

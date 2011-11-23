a = 0
f = (t = a, t1 = t, a = ++t1, t)

test = function () {
    return (f == 0) && (a == 1)
}
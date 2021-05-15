// KT-29088
fun compareIfBoolean(lhs: Any, rhs: Any): Int? {
    if (lhs !is Boolean) return null
    if (rhs !is Boolean) return null

    return lhs.compareTo(rhs)
}

fun box() : String {
    if (compareIfBoolean(true, false) != 1) return "Fail"
    return "OK"
}
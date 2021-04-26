package some

annotation class SomeAnnotation
open class Complete(@set:Some<caret> var field: Int) {
    interface XXX {
        val field: Int
    }
    class EEE(var field: Int) : XXX, Complete(2)
}

// ELEMENT: SomeAnnotation
// FIR_COMPARISON
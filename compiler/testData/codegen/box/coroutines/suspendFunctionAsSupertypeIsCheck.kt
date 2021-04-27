// WITH_RUNTIME
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// !LANGUAGE: +SuspendFunctionAsSupertype

import kotlin.coroutines.*

class C: suspend () -> Unit {
    override suspend fun invoke() {
    }
}

interface I: suspend () -> Unit {}

fun interface FI: suspend () -> Unit {}

fun box(): String {
    val c = C()
    if (c !is SuspendFunction0<Unit>) return "FAIL 1"
    if (c !is Function1<Continuation<Unit>, Any?>) return "FAIL 2"

    val i = object : I {
        override suspend fun invoke() {
        }
    }
    if (i !is SuspendFunction0<Unit>) return "FAIL 3"
    if (i !is Function1<Continuation<Unit>, Any?>) return "FAIL 4"

    val fi = object : FI {
        override suspend fun invoke() {
        }
    }
    if (fi !is SuspendFunction0<Unit>) return "FAIL 5"
    if (fi !is Function1<Continuation<Unit>, Any?>) return "FAIL 6"

    val o = object : suspend () -> Unit {
        override suspend fun invoke() {
        }
    }
    if (o !is SuspendFunction0<Unit>) return "FAIL 7"
    if (o !is Function1<Continuation<Unit>, Any?>) return "FAIL 8"

    return "OK"
}
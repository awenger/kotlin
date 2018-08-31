/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("URangesKt")

package kotlin.ranges

//
// NOTE: THIS FILE IS AUTO-GENERATED by the GenerateStandardLib.kt
// See: https://github.com/JetBrains/kotlin/tree/master/libraries/stdlib
//

import kotlin.*
import kotlin.text.*
import kotlin.comparisons.*
import kotlin.random.*

/**
 * Returns a random element from this range.
 * 
 * @throws IllegalArgumentException if this range is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UIntRange.random(): UInt {
    return random(Random)
}

/**
 * Returns a random element from this range.
 * 
 * @throws IllegalArgumentException if this range is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ULongRange.random(): ULong {
    return random(Random)
}

/**
 * Returns a random element from this range using the specified source of randomness.
 * 
 * @throws IllegalArgumentException if this range is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun UIntRange.random(random: Random): UInt {
    try {
        return random.nextUInt(this)
    } catch(e: IllegalArgumentException) {
        throw NoSuchElementException(e.message)
    }
}

/**
 * Returns a random element from this range using the specified source of randomness.
 * 
 * @throws IllegalArgumentException if this range is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun ULongRange.random(random: Random): ULong {
    try {
        return random.nextULong(this)
    } catch(e: IllegalArgumentException) {
        throw NoSuchElementException(e.message)
    }
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public infix fun UByte.downTo(to: UByte): UIntProgression {
    return UIntProgression.fromClosedRange(this.toUInt(), to.toUInt(), -1)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public infix fun UInt.downTo(to: UInt): UIntProgression {
    return UIntProgression.fromClosedRange(this, to, -1)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public infix fun ULong.downTo(to: ULong): ULongProgression {
    return ULongProgression.fromClosedRange(this, to, -1L)
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 * 
 * The [to] value should be less than or equal to `this` value.
 * If the [to] value is greater than `this` value the returned progression is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public infix fun UShort.downTo(to: UShort): UIntProgression {
    return UIntProgression.fromClosedRange(this.toUInt(), to.toUInt(), -1)
}

/**
 * Returns a progression that goes over the same range in the opposite direction with the same step.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun UIntProgression.reversed(): UIntProgression {
    return UIntProgression.fromClosedRange(last, first, -step)
}

/**
 * Returns a progression that goes over the same range in the opposite direction with the same step.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun ULongProgression.reversed(): ULongProgression {
    return ULongProgression.fromClosedRange(last, first, -step)
}

/**
 * Returns a progression that goes over the same range with the given step.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public infix fun UIntProgression.step(step: Int): UIntProgression {
    checkStepIsPositive(step > 0, step)
    return UIntProgression.fromClosedRange(first, last, if (this.step > 0) step else -step)
}

/**
 * Returns a progression that goes over the same range with the given step.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public infix fun ULongProgression.step(step: Long): ULongProgression {
    checkStepIsPositive(step > 0, step)
    return ULongProgression.fromClosedRange(first, last, if (this.step > 0) step else -step)
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public infix fun UByte.until(to: UByte): UIntRange {
    return this.toUInt() .. (to.toUInt() - 1u).toUInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 * 
 * If the [to] value is less than or equal to [UInt.MIN_VALUE] the returned range is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public infix fun UInt.until(to: UInt): UIntRange {
    if (to <= UInt.MIN_VALUE) return UIntRange.EMPTY
    return this .. (to - 1u).toUInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 * 
 * If the [to] value is less than or equal to [ULong.MIN_VALUE] the returned range is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public infix fun ULong.until(to: ULong): ULongRange {
    if (to <= ULong.MIN_VALUE) return ULongRange.EMPTY
    return this .. (to - 1u).toULong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 * 
 * If the [to] value is less than or equal to `this` value the returned range is empty.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public infix fun UShort.until(to: UShort): UIntRange {
    return this.toUInt() .. (to.toUInt() - 1u).toUInt()
}


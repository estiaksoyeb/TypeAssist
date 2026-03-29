package com.typeassist.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilityBeltTest {

    @Test
    fun testEvaluateMathSimple() {
        assertEquals("4", UtilityBelt.evaluateMath("2 + 2"))
        assertEquals("10", UtilityBelt.evaluateMath("5 * 2"))
        assertEquals("2.5", UtilityBelt.evaluateMath("5 / 2"))
        assertEquals("3", UtilityBelt.evaluateMath("10 - 7"))
    }

    @Test
    fun testEvaluateMathComplex() {
        assertEquals("14", UtilityBelt.evaluateMath("2 + 3 * 4"))
        assertEquals("20", UtilityBelt.evaluateMath("(2 + 3) * 4"))
        assertEquals("8", UtilityBelt.evaluateMath("2 ^ 3"))
    }

    @Test
    fun testEvaluateMathFunctions() {
        assertEquals("2", UtilityBelt.evaluateMath("sqrt(4)"))
        // sin(90) is 1.0
        assertEquals("1", UtilityBelt.evaluateMath("sin(90)"))
    }

    @Test
    fun testGeneratePassword() {
        val password = UtilityBelt.generatePassword()
        assertEquals(12, password.length)
        // Check if it contains some expected characters (random, but should be from the set)
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        assertTrue(password.all { it in chars })
    }

    @Test
    fun testEvaluateMathError() {
        assertEquals("Error", UtilityBelt.evaluateMath("invalid + expression"))
        assertEquals("Infinity", UtilityBelt.evaluateMath("5 / 0"))
    }
}

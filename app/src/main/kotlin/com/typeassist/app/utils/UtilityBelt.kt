package com.typeassist.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object UtilityBelt {

    fun getDate(): String {
        // Format: Friday, Dec 19
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getTime(): String {
        // Format: 2025-12-19 14:30
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    fun generatePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        return (1..12).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    fun evaluateMath(expression: String): String {
        return try {
            val result = object : Any() {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    ch = if (++pos < expression.length) expression[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    if (pos < expression.length) throw RuntimeException("Unexpected: " + ch.toChar())
                    return x
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        if (eat('+'.code)) x += parseTerm() // addition
                        else if (eat('-'.code)) x -= parseTerm() // subtraction
                        else return x
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        if (eat('*'.code)) x *= parseFactor() // multiplication
                        else if (eat('/'.code)) x /= parseFactor() // division
                        else return x
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+'.code)) return parseFactor() // unary plus
                    if (eat('-'.code)) return -parseFactor() // unary minus

                    var x: Double
                    val startPos = pos
                    if (eat('('.code)) { // parentheses
                        x = parseExpression()
                        eat(')'.code)
                    } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                        while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                        x = expression.substring(startPos, pos).toDouble()
                    } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                        while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                        val func = expression.substring(startPos, pos)
                        x = parseFactor()
                        x = when (func) {
                            "sqrt" -> Math.sqrt(x)
                            "sin" -> Math.sin(Math.toRadians(x))
                            "cos" -> Math.cos(Math.toRadians(x))
                            "tan" -> Math.tan(Math.toRadians(x))
                            "log" -> Math.log10(x)
                            else -> throw RuntimeException("Unknown function: $func")
                        }
                    } else {
                        throw RuntimeException("Unexpected: " + ch.toChar())
                    }

                    if (eat('^'.code)) x = Math.pow(x, parseFactor()) // exponentiation

                    return x
                }
            }.parse()
            
            // Return integer if it's a whole number, else decimal
            if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                // Format to a reasonable number of decimal places
                String.format("%.4f", result).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            "Error"
        }
    }
}

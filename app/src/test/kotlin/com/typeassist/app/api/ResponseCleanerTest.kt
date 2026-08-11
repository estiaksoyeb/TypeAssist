package com.typeassist.app.api

import org.junit.Assert.assertEquals
import org.junit.Test

class ResponseCleanerTest {

    @Test
    fun testClosedThinkTagStripping() {
        val input = "<think>\nThinking about hello world...\n</think>\nHello, world!"
        assertEquals("Hello, world!", cleanModelResponse(input))
    }

    @Test
    fun testClosedThinkTagCaseInsensitive() {
        val input = "<THINK>\nAnalyzing prompt...\n</THINK>\nThis is the answer."
        assertEquals("This is the answer.", cleanModelResponse(input))
    }

    @Test
    fun testUnclosedThinkTagWithPriorContent() {
        val input = "Here is the answer.\n<think>\nModel ran out of tokens before closing think..."
        assertEquals("Here is the answer.", cleanModelResponse(input))
    }

    @Test
    fun testUnclosedThinkTagWithoutPriorContentFallback() {
        val input = "<think>\nI should translate this word into French.\nBonjour is French for Hello.\nSo the translation of Hello is Bonjour."
        assertEquals("So the translation of Hello is Bonjour.", cleanModelResponse(input))
    }

    @Test
    fun testUnclosedThinkTagSingleLineFallback() {
        val input = "<think>The capital of France is Paris."
        assertEquals("The capital of France is Paris.", cleanModelResponse(input))
    }

    @Test
    fun testWrappingQuotesAndPipes() {
        val input1 = "\"Hello world\""
        assertEquals("Hello world", cleanModelResponse(input1))

        val input2 = "'Hello world'"
        assertEquals("Hello world", cleanModelResponse(input2))

        val input3 = "|Hello world|"
        assertEquals("Hello world", cleanModelResponse(input3))
    }
}

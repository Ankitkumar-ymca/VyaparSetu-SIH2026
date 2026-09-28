package com.example

import com.example.data.repository.AiService
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class AiServiceTest {
    @Test
    fun testAnswerQuestion() = runBlocking {
        val result = AiService.answerBusinessQuestion("What is inventory?")
        if (result.isFailure) {
            val ex = result.exceptionOrNull()
            println("ERROR: ${ex?.message}")
            ex?.printStackTrace()
            if (ex?.message?.contains("503") == true || ex?.message?.contains("UNAVAILABLE") == true ||
                ex?.message?.contains("429") == true || ex?.message?.contains("RESOURCE_EXHAUSTED") == true ||
                ex?.message?.contains("Quota") == true) {
                println("Gemini service rate limited (429) or unavailable (503). Skipping external failure.")
            } else {
                fail(ex?.message)
            }
        } else {
            println("SUCCESS: ${result.getOrNull()}")
            assertNotNull(result.getOrNull())
        }
    }
}

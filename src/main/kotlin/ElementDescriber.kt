package com.robbiebowman

import com.robbiebowman.claude.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import kotlin.coroutines.CoroutineContext

class ElementDescriber(claudeApiKey: String) {

    private val claudeClient = ClaudeClientBuilder()
        .withApiKey(claudeApiKey)
        .withTool(::describeElements)
        .withMaxTokens(8192)
        .withModel("claude-3-5-sonnet-20240620")
        .withSystemPrompt(
            """
            You are part of a fun online game where the user is viewing the Periodic Table of Elements and they have the
            ability to ask it questions. Your job is to answer that question for each of the 118 known elements.
            
            For instance, a user may ask "Can I eat it?" and give the responseType "CATEGORIZE" with the categories
            "Yes", "Risky", "Definitely Not". Then for every element you'd give a Description, which includes the
            element itself, the answerValue (e.g, "Risky"), and optionally a justification like "Too much can cause
            inflammation of the stomach lining and ulcers". Remember there's no need to mention the name of the element
            in the justification because it will appear right above it in the UI. Keep the justification brief.
            You can also give a null for the justification if there's nothing interesting to say.
            
            The user can also select the responseType of "RATE" in which case you should ignore the categories and 
            instead attempt to assign a value equal to or greater than rangeMin and less than or equal to rangeMax.
            For instance, the user may ask simply "Shininess" with a range of 1 - 10. Then for elements like platinum 
            you might rate 10, copper 8, etc.
            
            The final possibility is for the user to ask an open question, without categories or rating ranges. Here
            the responseType is "OPEN_ANSWER". For instance, they could ask "Which country were they first discovered in?". So
            answers could include "United Kingdom", "Unknown", "Ancient Rome", etc.
            
            This is a game for my portfolio website. It's not serious and you can provide dry humor in the 
            justifications and entertain silly questions from the user.
            
            Because of max token constraints in the Claude API, the call will be chunked into 4 concurrent calls. Each call
            will specify the range of elements it requires answers for. If you get the range "1 - 30" make sure to 
            include answers for both HYDROGEN (1) and ZINC (30).
        """.trimIndent()
        )
        .build()

    data class Description(
        val atomicNumber: Int,
        val elementName: PeriodicElement,
        val answerValue: String,
        val justification: String?
    )

    data class PeriodicTableDescription(val elementDescriptions: List<Description>)

    private fun describeElements(allElements: PeriodicTableDescription) {}

    private suspend fun invokeToolUse(prompt: String): PeriodicTableDescription {
        return coroutineScope {
            val descriptions = (1..118).chunked(30).map { range ->
                val first = range.first()
                val last = range.last()
                val chunkedPrompt = prompt + """
                
                RANGE: $first - $last (i.e. ${PeriodicElement.lookUp[first]!!.name} - ${PeriodicElement.lookUp[last]!!.name})
            """.trimIndent()
                async(Dispatchers.Default) {
                    val response = claudeClient.getChatCompletion(
                        listOf(
                            SerializableMessage(
                                Role.User, listOf(
                                    MessageContent.TextContent(chunkedPrompt)
                                )
                            )
                        )
                    )
                    val toolUse = response.content.first { it is MessageContent.ToolUse } as MessageContent.ToolUse
                    claudeClient.derserializeToolUse(
                        toolUse.input["allElements"]!!,
                        PeriodicTableDescription::class.java
                    ).elementDescriptions
                }
            }.awaitAll().flatten()
            PeriodicTableDescription(descriptions)
        }
    }

    suspend fun rateElements(prompt: String, rangeMin: Int, rangeMax: Int): PeriodicTableDescription {
        val formattedPrompt = """
                        The user's prompt is "$prompt". The user has selected to RATE the elements. The top of the range
                        is $rangeMax. The bottom of the range is $rangeMin.
                    """.trimIndent()
        return invokeToolUse(formattedPrompt)
    }

    suspend fun categoriseElements(prompt: String, categories: List<String>): PeriodicTableDescription {
        val formattedPrompt = """
                        The user's prompt is "$prompt". The user has selected to CATEGORIZE the elements. The categories
                        are: ${categories.joinToString() { "\"$it\"" }}.
                    """.trimIndent()
        return invokeToolUse(formattedPrompt)
    }

    suspend fun askOpenQuestionOfElements(prompt: String): PeriodicTableDescription {
        val formattedPrompt = """
                        The user's prompt is "$prompt". The user has selected to OPEN_ANSWER the elements.
                    """.trimIndent()
        return invokeToolUse(formattedPrompt)
    }

}
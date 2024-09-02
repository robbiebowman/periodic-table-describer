package com.robbiebowman

import com.robbiebowman.claude.*

class ElementDescriber(claudeApiKey: String) {

    private val claudeClient = ClaudeClientBuilder()
        .withApiKey(claudeApiKey)
        .withTool(::describeElements)
        .withMaxTokens(8192)
        .withModel("claude-3-5-sonnet-20240620")
        .withSystemPrompt("""
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
            
            Remember this is a game for my portfolio website. It's not serious and you can provide dry humor in the 
            justifications and entertain silly questions from the user.
        """.trimIndent())
        .build()

    data class Description(val element: PeriodicElement, val answerValue: String, val justification: String?)

    data class PeriodicTableDescription(val elementDescriptions: List<Description>)

    private fun describeElements(allElements: PeriodicTableDescription) {}

    fun categoriseElements(prompt: String, categories: List<String>): List<Description> {
        val response = claudeClient.getChatCompletion(
            listOf(
                SerializableMessage(
                    Role.User, listOf(
                        MessageContent.TextContent(
                            """
                        The user's prompt is "$prompt". The user has selected to CATEGORIZE the elements. The categories
                        are: ${categories.joinToString() { "\"$it\"" }}
                    """.trimIndent()
                        )
                    )
                )
            )
        )
        val toolUse = response.content.first { it is MessageContent.ToolUse } as MessageContent.ToolUse
        val description = claudeClient.derserializeToolUse(toolUse.input["categoriseElements"]!!, PeriodicTableDescription::class.java)
        return description.elementDescriptions
    }

}
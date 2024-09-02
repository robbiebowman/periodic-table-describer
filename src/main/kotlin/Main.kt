package com.robbiebowman

import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val service = ElementDescriber(System.getenv("CLAUDE_API_KEY"))
        val table =
            service.askOpenQuestionOfElements(
                "Where were they discovered?"
            )
        println(table.elementDescriptions.joinToString("\n") { it.toString() })
    }
}
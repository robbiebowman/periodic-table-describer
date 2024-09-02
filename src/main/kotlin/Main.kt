package com.robbiebowman

fun main() {
    val service = ElementDescriber(System.getenv("CLAUDE_API_KEY"))
    val table =
        service.askOpenQuestionOfElements(
            "Who discovered them?"
        )
    println(table.elementDescriptions.joinToString("\n") { it.toString() })
}
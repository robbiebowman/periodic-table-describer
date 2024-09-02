package com.robbiebowman

fun main() {
    val service = ElementDescriber(System.getenv("CLAUDE_API_KEY"))
    val descriptions =
        service.categoriseElements("Could I build a house out of this?", listOf("Suitable", "Difficult byt could work", "Don't even try"))
    println(descriptions.joinToString("\n") { it.toString() })
}
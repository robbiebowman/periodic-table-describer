import com.fasterxml.jackson.core.type.TypeReference
import com.robbiebowman.claude.MessageContent
import com.robbiebowman.claudeMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class ClaudeMapperTest {

    @Test
    fun ignoresThinkingBeforeToolUse() {
        val content = claudeMapper().readValue(
            """[
                {"type":"thinking","thinking":"reasoning","signature":"signature"},
                {"type":"tool_use","id":"tool-1","name":"describeElements","input":{"allElements":{"elementDescriptions":[]}}}
            ]""".trimIndent(),
            object : TypeReference<List<MessageContent?>>() {}
        )

        assertEquals("describeElements", content.filterIsInstance<MessageContent.ToolUse>().single().name)
    }
}

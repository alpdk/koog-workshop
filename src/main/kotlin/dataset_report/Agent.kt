package ai.koog.workshop.dataset_report

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val token = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY not set")
    val reporter = DatasetTools()

    val jsonPrompt = """
        You are a helpful assistant for dataset analysis.
        You can read CSV files, compute statistics, and save reports.
        Only use tools when necessary.
        Keep conversation context in mind.
    """.trimIndent()

    val history = mutableListOf<String>()
    history.add("system: $jsonPrompt")

    println("=== Dataset Assistant (Chat Mode) ===")
    println("Type 'exit' to quit.")
    println("-------------------------------------")

    while (true) {
        print("You: ")
        val userInput = readLine().orEmpty()
        if (userInput.lowercase() == "exit") break

        history.add("user: $userInput")

        // Recreate the agent every turn because singleRunStrategy can be run only once
        val agent = AIAgent(
            toolRegistry = ToolRegistry { tools(reporter.asTools()) },
            agentConfig = AIAgentConfig(
                prompt = prompt("dataset-agent") {
                    system(history.joinToString("\n"))
                },
                model = OpenAIModels.Chat.GPT4o,
                maxAgentIterations = 10
            ),
            promptExecutor = simpleOpenAIExecutor(token),
            strategy = ai.koog.agents.core.agent.singleRunStrategy()
        )

        try {
            val reply = agent.run(userInput)
            println("Agent: $reply")
            history.add("assistant: $reply")

            if (reply.contains("__EXIT__")) {
                println("Agent requested exit.")
                break
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }

    println("Goodbye!")
}

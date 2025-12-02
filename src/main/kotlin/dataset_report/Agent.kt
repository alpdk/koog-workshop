package ai.koog.workshop.dataset_report

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val token = System.getenv("OPENAI_API_KEY")
    if (token.isNullOrBlank()) {
        println("Error: OPENAI_API_KEY is not set.")
        return@runBlocking
    }

    val reporter = DatasetTools()

    val agent = AIAgent(
        toolRegistry = ToolRegistry {
            tools(reporter.asTools())
        },
        agentConfig = AIAgentConfig(
            prompt = prompt("dataset-agent") {
                system("""
                    You are a helpful assistant for dataset analysis.
                    You can read CSV files, compute statistics, and save reports.
                    Only use tools when necessary.
                    Keep previous conversation context in mind.
                """.trimIndent())
            },
            model = OpenAIModels.Chat.GPT4o,
            maxAgentIterations = 10
        ),
        promptExecutor = simpleOpenAIExecutor(token),
        strategy = ai.koog.agents.core.agent.singleRunStrategy() // keep single-run for now; later can replace with chat strategy if available
    )

    println("=== Dataset Assistant (Chat Mode) ===")
    println("Type 'exit' to quit.")
    println("-------------------------------------")
    
    while (true) {
        print("You: ")
        val userInput = readLine().orEmpty()
        if (userInput.lowercase() == "exit") break

        try {
            val reply = agent.run(userInput)
            println("Agent: $reply")
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }

    println("Goodbye!")
}

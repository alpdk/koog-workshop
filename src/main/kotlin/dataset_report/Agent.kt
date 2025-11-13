package ai.koog.workshop.dataset_report


import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.dsl.prompt
import ai.koog.agents.core.agent.singleRunStrategy


import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.clients.openai.OpenAIModels


import kotlinx.coroutines.runBlocking


fun main() = runBlocking {
    val token = System.getenv("OPENAI_API_KEY")
    val reporter = ReadCsvTool()

    val agent = AIAgent(
        toolRegistry = ToolRegistry {
            tools(reporter.asTools())
        },
        agentConfig = AIAgentConfig(
            prompt = prompt("dataset-agent") {
                system("You are a helpful dataset analysis assistant.")
            },
            model = OpenAIModels.Chat.GPT4o,
            maxAgentIterations = 10
        ),
        promptExecutor = simpleOpenAIExecutor(token),
        strategy = singleRunStrategy()
    )


    val csvPath = "/home/alpdk/gitRepos/koog-workshop/src/main/resources/housing.csv" // replace with your CSV file path
    val reportPath = "/out/dataset_report.md" // desired output path

    val query = "Please, read the dataset ${csvPath}, and print odd samples with sort of them by price"


    val result = try {
        agent.run(query)
    } catch (e: Exception) {
        e.printStackTrace()
        "Error: ${'$'}{e.message}"
    }


    println("========================")
    println(result)
    println("========================")
}

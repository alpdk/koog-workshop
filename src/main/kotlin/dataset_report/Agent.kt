package ai.koog.workshop.dataset_report

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTool
import ai.koog.prompt.dsl.prompt
import ai.koog.agents.core.agent.singleRunStrategy


// Import for Ollama support
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.OllamaModels

import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLMCapability

import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    val llama3 = LLModel(
        provider = LLMProvider.Ollama,
        id = "llama3:latest",
        capabilities = listOf(
            LLMCapability.Temperature,
            LLMCapability.Schema.JSON.Standard,
            LLMCapability.Tools
        ),
        contextLength = 8192
    )

    val agent = AIAgent(
//        toolRegistry = ToolRegistry {
//            tool(::readDataset.asTool())
//            tool(::analyzeMissingValues.asTool())
//            tool(::inferDataTypes.asTool())
//            tool(::suggestReplacements.asTool())
//            tool(::generateMarkdownReport.asTool())
//        },
        agentConfig = AIAgentConfig(
            prompt = prompt("dataset-agent") {
                system("You are a helpful zoo assistance.")
            },
            model = llama3,
            maxAgentIterations = 100
        ),
        promptExecutor = simpleOllamaAIExecutor(),
        strategy = singleRunStrategy()
    )

    val result = try {
        agent.run("Tell me please about crocodiles")
    } catch (e: Exception) {
        e.printStackTrace()
        "Error: ${e.message}"
    }
    println("========================")
    println(result)
    println("========================")
}

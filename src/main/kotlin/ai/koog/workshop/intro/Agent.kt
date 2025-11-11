package ai.koog.workshop.intro

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTool
import ai.koog.prompt.dsl.prompt

// Import for Ollama support
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.ollama.client.OllamaModelCard
import ai.koog.prompt.llm.OllamaModels

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val agent = AIAgent(
        toolRegistry = ToolRegistry {
            tool(::doNothingTool.asTool())
        },
        strategy = strategy("my-first-strategy") {
            val myNode by node<String, String> {
                input ->
                // Call the tool first
                val toolOutput = doNothingTool(input)
                println("Tool returned: $toolOutput")

                // Return the original input (or modify as needed)
                input
            }
            edge(nodeStart forwardTo myNode)
            edge(myNode forwardTo nodeFinish)
        },
        agentConfig = AIAgentConfig(
            prompt = prompt("my‑first‑agent") {
                system("Your are helpful assistant about travel in Germany")
            },
            // Here you may still need to supply some model object –
            // if the API expects it non‑null you might need a dummy or default LLModel instance
            model = OllamaModels.Meta.LLAMA_3_2,
            maxAgentIterations = 100
        ),
        promptExecutor = simpleOllamaAIExecutor()
    )

    val result = agent.run("What castles I can visit in Germany?")
    println("========================")
    println(result)
    println("========================")
}

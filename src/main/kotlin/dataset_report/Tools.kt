package ai.koog.workshop.dataset_report

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import java.io.File
import kotlin.random.Random

/**
 * Gets the price of an item (mocked, returns a random number)
 */
// You can define the tool custom name, otherwise the name of the function will be used
@Tool("get_price")
// Description of the tool, it will be used by the LLM to know what to call
@LLMDescription("Get the price of an item.")
fun getPrice(item: String): Int {
    return Random.nextInt(100)
}
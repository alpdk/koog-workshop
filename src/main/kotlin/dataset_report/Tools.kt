package ai.koog.workshop.dataset_report

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.random.Random
import java.io.BufferedReader
import java.io.FileReader

@Serializable
data class ReadCsvResult(
    val fileName: String,
    val headers: List<String>,
    val rowCount: Long,
    val sampleRows: List<Map<String, String>>
)


class ReadCsvTool : ToolSet {
    @Tool("read_csv")
    @LLMDescription("Reads the CSV file and returns headers, total row count and up to 10 sample rows.")
    fun readCsv(path: String): ReadCsvResult {
        val file = File(path)
        require(file.exists()) { "CSV file not found: $path" }

        val reader = BufferedReader(FileReader(file))
        val headers = reader.readLine()?.split(",")?.map { it.trim() } ?: emptyList()
        val sample = mutableListOf<Map<String, String>>()
        var rowCount = 0L

        reader.lineSequence().forEach { line ->
            rowCount += 1
            if (sample.size < 10) {
                val values = line.split(",")
                val rowMap = headers.zip(values) { h, v -> h to v }.toMap()
                sample.add(rowMap)
            }
        }
        reader.close()

        return ReadCsvResult(fileName = file.name, headers = headers, rowCount = rowCount, sampleRows = sample)
    }
}
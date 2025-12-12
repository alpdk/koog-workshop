package ai.koog.workshop.dataset_report

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.serialization.Serializable
import java.io.File
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.BufferedReader
import java.io.FileReader
import kotlin.sequences.forEach

@Serializable
data class ReadCsvResult(
    val fileName: String,
    val headers: List<String>,
    val rowCount: Long,
    val sampleRows: List<Map<String, String>>
)

@Serializable
data class ColumnStat(
    val name: String,
    val inferredType: String,
    val missing: Long,
    val uniqueCount: Long,
    val numericMin: Double? = null,
    val numericMax: Double? = null,
    val sampleValues: List<String> = emptyList()
)

@Serializable
data class StatsResult(
    val fileName: String,
    val rowCount: Long,
    val columnCount: Int,
    val columns: List<ColumnStat>
)

class DatasetTools : ToolSet {

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

    @Tool("compute_stats")
    @LLMDescription("Compute basic per-column statistics for the CSV file: missing values, inferred types, unique counts, numeric min/max.")
    fun computeStats(path: String): StatsResult {
        val rows = readCsvProper(path)
        if (rows.isEmpty()) return StatsResult(File(path).name, 0, 0, emptyList())

        val headers = rows.first().keys.toList()
        val trackers = headers.associateWith { Tracker() }

        rows.forEach { row ->
            headers.forEach { h ->
                val t = trackers[h]!!
                val value = row[h]?.trim() ?: ""
                t.total += 1
                if (value.isEmpty()) t.missing += 1
                else {
                    t.uniques.add(value)
                    if (t.samples.size < 5) t.samples.add(value)
                    val d = value.toDoubleOrNull()
                    if (d != null) {
                        t.numericCount += 1
                        t.min = if (t.min == null || d < t.min!!) d else t.min
                        t.max = if (t.max == null || d > t.max!!) d else t.max
                    }
                }
            }
        }

        val columns = headers.map { h ->
            val t = trackers[h]!!
            val inferred = when {
                t.total == 0L -> "unknown"
                t.numericCount.toDouble() / t.total >= 0.9 -> "numeric"
                else -> "string"
            }
            ColumnStat(
                name = h,
                inferredType = inferred,
                missing = t.missing,
                uniqueCount = t.uniques.size.toLong(),
                numericMin = t.min,
                numericMax = t.max,
                sampleValues = t.samples.toList()
            )
        }

        return StatsResult(fileName = File(path).name, rowCount = rows.size.toLong(), columnCount = headers.size, columns = columns)
    }

    @Tool("save_file")
    @LLMDescription("Save text content to a file path. Returns the absolute path of the saved file.")
    fun saveFile(path: String, content: String): String {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
        return file.absolutePath
    }

    @Tool("save_stats_md")
    @LLMDescription("Saves computed dataset statistics to a Markdown file.")
    fun saveStatsAsMd(stats: StatsResult, path: String): String {
        val sb = StringBuilder()
        sb.appendLine("# Dataset Report: ${stats.fileName}")
        sb.appendLine()
        sb.appendLine("- Total rows: ${stats.rowCount}")
        sb.appendLine("- Total columns: ${stats.columnCount}")
        sb.appendLine()
        sb.appendLine("## Columns")
        sb.appendLine("| Name | Type | Missing | Unique | Min | Max | Sample values |")
        sb.appendLine("|------|------|---------|--------|-----|-----|---------------|")

        stats.columns.forEach { col ->
            val minStr = col.numericMin?.toString() ?: ""
            val maxStr = col.numericMax?.toString() ?: ""
            val samples = col.sampleValues.joinToString(", ") { escapeMd(it) }
            sb.appendLine("| ${escapeMd(col.name)} | ${col.inferredType} | ${col.missing} | ${col.uniqueCount} | $minStr | $maxStr | $samples |")
        }

        return saveFile(path, sb.toString())
    }

    @Tool("exit_chat")
    @LLMDescription("End the conversation. Returns special token '__EXIT__'.")
    fun exitChat(message: String? = null): String {
        return if (message.isNullOrBlank()) "__EXIT__" else "__EXIT__ $message"
    }

    fun readCsvProper(path: String): List<Map<String, String>> {
        val file = File(path)
        require(file.exists()) { "CSV file not found: $path" }

        val rows = mutableListOf<Map<String, String>>()
        csvReader().open(file) {
            val header = readNext() ?: emptyList()
            readAllAsSequence().forEach { row ->
                rows.add(header.zip(row) { h, v -> h to v }.toMap())
            }
        }
        return rows
    }

    private class Tracker(
        var missing: Long = 0,
        var total: Long = 0,
        var numericCount: Long = 0,
        val uniques: MutableSet<String> = mutableSetOf(),
        var min: Double? = null,
        var max: Double? = null,
        val samples: MutableList<String> = mutableListOf()
    )

    private fun escapeMd(s: String): String =
        s.replace("|", "\\|")
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()
}

package ai.koog.workshop.dataset_report

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import java.io.File

@Tool
@LLMDescription("Reads a CSV dataset from the given file path and returns basic info about it.")
fun readDataset(csvPath: String): List<List<String>> {
    val file = File(csvPath)
    require(file.exists()) { "File not found: $csvPath" }

    val lines = file.readLines()

    if (lines.isEmpty()) return emptyList()

    return lines.map { it.split(",").map { cell -> cell.trim() } }
}

@Tool
@LLMDescription("Analyzes missing values in each column of a CSV dataset (rows are List<List<String>>). Returns a map of column index to missing count.")
fun analyzeMissingValues(data: List<List<String>>): Map<Int, Int> {
    if (data.isEmpty()) return emptyMap()

    val columnCount = data.first().size

    return (0 until columnCount).associateWith { colIndex ->
        data.count { row -> row.getOrNull(colIndex).isNullOrBlank() }
    }
}

@Tool
@LLMDescription("Infers the data type of each column: numeric, boolean, or string.")
fun inferDataTypes(data: List<List<String>>): Map<Int, String> {
    if (data.isEmpty()) return emptyMap()

    val columnCount = data.first().size

    return (0 until columnCount).associateWith { colIndex ->
        val values = data.mapNotNull { it.getOrNull(colIndex) }.filter { it.isNotBlank() }
        if (values.all { it.toDoubleOrNull() != null }) "numeric"
        else if (values.all { it.equals("true", true) || it.equals("false", true) }) "boolean"
        else "string"
    }
}

@Tool
@LLMDescription("Suggests replacement values for missing entries for each column based on type (mean for numeric, mode for string).")
fun suggestReplacements(
    data: List<List<String>>,
    columnTypes: Map<Int, String>
): Map<Int, String> {
    if (data.isEmpty()) return emptyMap()

    val columnCount = data.first().size

    return (0 until columnCount).associateWith { colIndex ->
        val type = columnTypes[colIndex] ?: "string"
        val values = data.mapNotNull { it.getOrNull(colIndex) }.filter { it.isNotBlank() }

        when (type) {
            "numeric" -> {
                val nums = values.mapNotNull { it.toDoubleOrNull() }
                if (nums.isEmpty()) "0"
                else "%.2f".format(nums.average())
            }

            "string" -> {
                values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: ""
            }

            "boolean" -> {
                if (values.count { it.equals("true", true) } >= values.count {
                        it.equals(
                            "false",
                            true
                        )
                    }) "true" else "false"
            }

            else -> ""
        }
    }
}


@Tool
@LLMDescription("Generates a Markdown report from the dataset, missing value counts, types, and suggested replacements.")
fun generateMarkdownReport(
    data: List<List<String>>,
    missingCounts: Map<Int, Int>,
    columnTypes: Map<Int, String>,
    replacementSuggestions: Map<Int, String>,
    columnNames: List<String>
): String {
    val report = StringBuilder("# 🧾 Dataset Report\n\n")

    report.appendLine("**Rows:** ${data.size}")
    report.appendLine("**Columns:** ${columnNames.size}\n")
    report.appendLine("## Column Details\n")

    for (i in columnNames.indices) {
        val colName = columnNames[i]
        val missing = missingCounts[i] ?: 0
        val type = columnTypes[i] ?: "unknown"
        val suggestion = replacementSuggestions[i] ?: "N/A"
        report.appendLine("### `$colName`")
        report.appendLine("- Type: `$type`")
        report.appendLine("- Missing: $missing")
        report.appendLine("- Suggested replacement: $suggestion\n")
    }

    return report.toString()
}
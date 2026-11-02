package id.algaffar.jwstv.data.excel

import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.InputStream
import java.io.OutputStream
import kotlin.streams.toList

data class TakjilImportPreviewRow(
    val day: Int,
    val names: List<String>
)

/**
 * Handles exporting the 30-Day Ramadhan Ta'jil Excel template (`.xlsx`) and
 * importing/parsing `.xlsx` schedules (for all 30 days or a specific target day).
 * Mirrors `TakjilImportModal` & `XLSX` logic in `src/components/Settings/RamadhanSettingsForm.tsx`.
 */
object TakjilExcelManager {

    /**
     * Generates a ready-to-fill `.xlsx` template for 30 days of Ramadhan Ta'jil donors.
     */
    fun exportTemplateExcel(
        outputStream: OutputStream,
        existingTakjil: Map<String, List<String>> = emptyMap()
    ) {
        Workbook(outputStream, "JWS TV Al-Gaffar", "2.0").use { wb ->
            val ws = wb.newWorksheet("Jadwal Takjil Ramadhan")
            ws.width(0, 14.0)
            ws.width(1, 55.0)
            ws.width(2, 30.0)
            ws.width(3, 30.0)

            // Header row
            ws.value(0, 0, "Hari Ke")
            ws.value(0, 1, "Nama Penyumbang 1 (atau pisahkan dengan koma)")
            ws.value(0, 2, "Nama Penyumbang 2")
            ws.value(0, 3, "Nama Penyumbang 3")
            ws.style(0, 0).bold().set()
            ws.style(0, 1).bold().set()
            ws.style(0, 2).bold().set()
            ws.style(0, 3).bold().set()

            for (day in 1..30) {
                val rowIdx = day
                ws.value(rowIdx, 0, day.toDouble())
                val currentNames = existingTakjil[day.toString()].orEmpty()
                if (currentNames.isNotEmpty()) {
                    currentNames.forEachIndexed { idx, name ->
                        ws.value(rowIdx, idx + 1, name)
                    }
                } else if (day == 1) {
                    ws.value(rowIdx, 1, "H. Ahmad Fauzi")
                    ws.value(rowIdx, 2, "Keluarga Bpk. Ridwan")
                }
            }
            wb.finish()
        }
    }

    /**
     * Parses an uploaded `.xlsx` file into a list of [TakjilImportPreviewRow]s.
     * Supports two formats (matching the legacy Tauri app):
     * - Format A (Full 30-day table): Column A is day number (`1..30` or `"Hari ke-1"`), Columns B+ are donor names.
     * - Format B (Single target day when [targetDay] != null): Every cell/row contains donor names for [targetDay].
     */
    fun parseTakjilExcel(
        inputStream: InputStream,
        targetDay: Int? = null
    ): List<TakjilImportPreviewRow> {
        ReadableWorkbook(inputStream).use { wb ->
            val sheet = wb.firstSheet
            val rows = sheet.openStream().toList()
            if (rows.isEmpty()) return emptyList()

            if (targetDay != null && targetDay in 1..30) {
                val collectedNames = mutableListOf<String>()
                rows.forEachIndexed { rowIndex, row ->
                    for (colIndex in 0 until row.cellCount) {
                        val raw = row.getCellText(colIndex)?.trim().orEmpty()
                        if (raw.isBlank()) continue
                        if (rowIndex == 0 && isHeaderCell(raw)) continue
                        val dayCandidate = parseDayNumber(raw)
                        if (colIndex == 0 && dayCandidate != null) continue
                        raw.split(",", ";")
                            .map { it.trim() }
                            .filter { it.isNotBlank() && !isHeaderCell(it) }
                            .forEach { collectedNames.add(it) }
                    }
                }
                return if (collectedNames.isNotEmpty()) {
                    listOf(TakjilImportPreviewRow(day = targetDay, names = collectedNames.distinct()))
                } else {
                    emptyList()
                }
            }

            // Full 30-day table mode
            val byDay = linkedMapOf<Int, MutableList<String>>()
            rows.forEachIndexed { rowIndex, row ->
                if (row.cellCount == 0) return@forEachIndexed
                val firstCell = row.getCellText(0)?.trim().orEmpty()
                if (rowIndex == 0 && isHeaderCell(firstCell)) return@forEachIndexed

                val dayNumber = parseDayNumber(firstCell) ?: (rowIndex + 1).takeIf { rowIndex < 30 && firstCell.isNotBlank() }
                if (dayNumber != null && dayNumber in 1..30) {
                    val namesForRow = mutableListOf<String>()
                    val startCol = if (parseDayNumber(firstCell) != null) 1 else 0
                    for (colIndex in startCol until row.cellCount) {
                        val cellText = row.getCellText(colIndex)?.trim().orEmpty()
                        if (cellText.isNotBlank()) {
                            cellText.split(",", ";")
                                .map { it.trim() }
                                .filter { it.isNotBlank() && !isHeaderCell(it) }
                                .forEach { namesForRow.add(it) }
                        }
                    }
                    if (namesForRow.isNotEmpty()) {
                        byDay.getOrPut(dayNumber) { mutableListOf() }.addAll(namesForRow)
                    }
                }
            }

            return byDay.entries
                .sortedBy { it.key }
                .map { (day, names) -> TakjilImportPreviewRow(day = day, names = names.distinct()) }
        }
    }

    private fun parseDayNumber(raw: String): Int? {
        val cleaned = raw.lowercase()
            .replace("hari ke-", "")
            .replace("hari ke", "")
            .replace("malam ke-", "")
            .replace("malam ke", "")
            .replace("ramadhan", "")
            .trim()
        val asDouble = cleaned.toDoubleOrNull()
        if (asDouble != null) {
            val asInt = asDouble.toInt()
            if (asInt in 1..30) return asInt
        }
        return null
    }

    private fun isHeaderCell(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower == "hari" ||
            lower == "hari ke" ||
            lower == "no" ||
            lower.startsWith("nama penyumbang") ||
            lower == "nama" ||
            lower == "donatur"
    }
}

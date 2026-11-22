package id.algaffar.jwstv.data.cctv

import id.algaffar.jwstv.domain.model.JumatKeuanganRecord
import id.algaffar.jwstv.domain.model.JumatKeuanganSettings
import id.algaffar.jwstv.domain.model.OnlineFinanceSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Serializable
data class FinancialTransactionItem(
    val date: String = "",
    val description: String = "",
    val amount: Long = 0L,
    val type: String = "in" // "in" | "out"
)

@Serializable
data class FinancialSummaryData(
    val balance: Long = 0L,
    val income: Long = 0L,
    val expense: Long = 0L,
    val lastUpdated: String = "",
    val isLocalFallback: Boolean = false,
    val recentTransactions: List<FinancialTransactionItem> = emptyList()
)

/**
 * Online Mosque Finance Synchronizer (Google Sheets CSV / JSON REST endpoint with automatic local fallback).
 * Mirrors `src/hooks/useOnlineFinance.ts`.
 */
class OnlineFinanceManager(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpClient: OkHttpClient = id.algaffar.jwstv.SharedHttpClient.baseClient
) {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun fetchFinancialData(
        onlineFinance: OnlineFinanceSettings,
        localJumatKeuangan: JumatKeuanganSettings
    ): FinancialSummaryData = withContext(ioDispatcher) {
        if (!onlineFinance.enabled || onlineFinance.sheetUrl.isBlank()) {
            return@withContext buildLocalFallback(localJumatKeuangan)
        }

        try {
            val resolvedUrl = normalizeGoogleSheetsUrl(onlineFinance.sheetUrl)
            val request = Request.Builder()
                .url(resolvedUrl)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext buildLocalFallback(localJumatKeuangan)
                }
                val body = response.body?.string()?.trim().orEmpty()
                if (body.isEmpty()) {
                    return@withContext buildLocalFallback(localJumatKeuangan)
                }

                if (body.startsWith("{")) {
                    jsonParser.decodeFromString<FinancialSummaryData>(body)
                        .copy(isLocalFallback = false)
                } else {
                    parseFinanceCsv(body)
                }
            }
        } catch (_: Exception) {
            buildLocalFallback(localJumatKeuangan)
        }
    }

    companion object {
        /**
         * Auto-converts Google Sheets `/pubhtml` and `/edit` links into direct CSV export URLs.
         */
        fun normalizeGoogleSheetsUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.contains("docs.google.com/spreadsheets")) {
                if (trimmed.endsWith("/pubhtml")) {
                    return trimmed.removeSuffix("/pubhtml") + "/pub?output=csv"
                }
                if (trimmed.contains("/edit")) {
                    return trimmed.substringBefore("/edit") + "/export?format=csv"
                }
            }
            return trimmed
        }

        /**
         * Parses CSV rows with columns: `Tanggal, Keterangan, Masuk, Keluar`.
         * Handles quoted fields containing commas and Indonesian IDR formatting (`1.500.000` -> `1500000`).
         */
        fun parseFinanceCsv(csvText: String, todayDateStr: String = LocalDate.now().toString()): FinancialSummaryData {
            val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size <= 1) {
                return FinancialSummaryData(lastUpdated = todayDateStr, isLocalFallback = false)
            }

            val csvSplitRegex = Regex(""",(?=(?:[^"]*"[^"]*")*[^"]*$)""")
            val transactions = mutableListOf<FinancialTransactionItem>()

            for (line in lines.drop(1)) {
                val cols = line.split(csvSplitRegex).map { it.trim().removeSurrounding("\"").trim() }
                val date = cols.getOrNull(0).orEmpty()
                val description = cols.getOrNull(1).orEmpty()
                val masuk = parseIdrAmount(cols.getOrNull(2))
                val keluar = parseIdrAmount(cols.getOrNull(3))

                val amount = if (masuk > 0L) masuk else keluar
                if (amount > 0L) {
                    transactions.add(
                        FinancialTransactionItem(
                            date = date,
                            description = description,
                            amount = amount,
                            type = if (masuk > 0L) "in" else "out"
                        )
                    )
                }
            }

            val totalIncome = transactions.filter { it.type == "in" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == "out" }.sumOf { it.amount }

            return FinancialSummaryData(
                balance = totalIncome - totalExpense,
                income = totalIncome,
                expense = totalExpense,
                lastUpdated = todayDateStr,
                isLocalFallback = false,
                recentTransactions = transactions.takeLast(5).reversed()
            )
        }

        fun buildLocalFallback(
            keuangan: JumatKeuanganSettings,
            fallbackDateStr: String = LocalDate.now().toString()
        ): FinancialSummaryData {
            val selectedDate = keuangan.selectedDate.ifBlank { fallbackDateStr }
            val record: JumatKeuanganRecord = keuangan.records[selectedDate]
                ?: keuangan.records.values.lastOrNull()
                ?: JumatKeuanganRecord()

            val income = record.pemasukan
            val expense = record.pengeluaran
            val initialKas = if (record.saldoKasLalu != 0L) record.saldoKasLalu else keuangan.saldoKasLalu
            val balance = initialKas + (income - expense)

            val recent = record.entries.takeLast(5).reversed().map { entry ->
                FinancialTransactionItem(
                    date = selectedDate,
                    description = entry.description,
                    amount = entry.amount,
                    type = entry.type
                )
            }

            return FinancialSummaryData(
                balance = balance,
                income = income,
                expense = expense,
                lastUpdated = selectedDate,
                isLocalFallback = true,
                recentTransactions = recent
            )
        }

        internal fun parseIdrAmount(raw: String?): Long {
            if (raw.isNullOrBlank()) return 0L
            val digitsOnly = raw.replace(Regex("""[^\d]"""), "")
            return digitsOnly.toLongOrNull() ?: 0L
        }
    }
}

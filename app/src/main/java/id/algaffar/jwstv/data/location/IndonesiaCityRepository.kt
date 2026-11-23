package id.algaffar.jwstv.data.location

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

@Serializable
data class CityPreset(
    @SerialName("p") val province: String,
    @SerialName("c") val city: String,
    @SerialName("lat") val latitude: Double,
    @SerialName("lng") val longitude: Double,
    @SerialName("tz") val timezone: String
)

/**
 * Offline database of Indonesian Provinces & Cities/Regencies (all 38 provinces + ASEAN presets)
 * so mosque caretakers never have to manually type latitude/longitude via a TV D-Pad remote.
 */
object IndonesiaCityRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    private var cachedCities: List<CityPreset>? = null

    fun loadCities(context: Context? = null): List<CityPreset> {
        cachedCities?.let { return it }
        val loaded = runCatching {
            context?.assets?.open("data/indonesia_cities.json")?.use { stream ->
                parseFromStream(stream)
            }
        }.getOrNull().orEmpty()

        val result = loaded.ifEmpty { FALLBACK_MAJOR_CITIES }
        cachedCities = result
        return result
    }

    fun parseFromStream(inputStream: InputStream): List<CityPreset> {
        val raw = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return json.decodeFromString<List<CityPreset>>(raw)
    }

    fun getProvinces(cities: List<CityPreset>): List<String> =
        cities.map { it.province }.distinct()

    fun getCitiesInProvince(cities: List<CityPreset>, province: String): List<CityPreset> =
        cities.filter { it.province.equals(province, ignoreCase = true) }

    fun search(cities: List<CityPreset>, query: String, limit: Int = 20): List<CityPreset> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return cities.take(limit)
        return cities.filter {
            it.city.lowercase().contains(q) || it.province.lowercase().contains(q)
        }.take(limit)
    }

    private val FALLBACK_MAJOR_CITIES = listOf(
        CityPreset("DKI Jakarta", "Jakarta Pusat", -6.1862, 106.8341, "Asia/Jakarta"),
        CityPreset("Jawa Barat", "Kota Bandung", -6.9175, 107.6191, "Asia/Jakarta"),
        CityPreset("Jawa Tengah", "Kota Semarang", -6.9667, 110.4167, "Asia/Jakarta"),
        CityPreset("DI Yogyakarta", "Kota Yogyakarta", -7.7956, 110.3695, "Asia/Jakarta"),
        CityPreset("Jawa Timur", "Kota Surabaya", -7.2575, 112.7521, "Asia/Jakarta"),
        CityPreset("Kalimantan Timur", "Kota Balikpapan", -1.2379, 116.8529, "Asia/Makassar"),
        CityPreset("Sulawesi Selatan", "Kota Makassar", -5.1477, 119.4327, "Asia/Makassar"),
        CityPreset("Sumatera Utara", "Kota Medan", 3.5952, 98.6722, "Asia/Jakarta"),
        CityPreset("Papua", "Kota Jayapura", -2.5916, 140.6690, "Asia/Jayapura")
    )
}

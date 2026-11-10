package com.netcar.app.data

import com.netcar.app.model.Expense
import com.netcar.app.model.ExpenseCategory
import com.netcar.app.model.FillUp
import com.netcar.app.model.FuelType
import com.netcar.app.model.Units
import com.netcar.app.model.Vehicle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class FuelioImportResult(
    val fillUps: List<FillUp>,
    val expenses: List<Expense>,
    val maxOdometer: Int
)

object FuelioCsvHelper {

    fun parseCsvLine(line: String, delimiter: Char = ','): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (ch == delimiter && !inQuotes) {
                result.add(current.toString().trim())
                current.clear()
            } else {
                current.append(ch)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    private fun escapeCsvCell(value: Any?): String {
        if (value == null) return "\"\""
        val escaped = value.toString().replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun mapFuelioCodeToFuelType(raw: String?, fallback: FuelType = FuelType.GASOLINE): FuelType {
        if (raw.isNullOrBlank()) return fallback
        val v = raw.trim().lowercase(Locale.US)
        if (v == "diesel" || v == "gazole" || v == "gasoil") return FuelType.DIESEL
        if (v == "gasoline" || v == "petrol" || v == "essence" || v == "sp95" || v == "sp98") return FuelType.GASOLINE
        if (v == "lpg" || v == "gpl") return FuelType.LPG
        if (v == "electric" || v == "ev" || v == "électrique") return FuelType.ELECTRIC
        if (v == "hybrid" || v == "hybride") return FuelType.HYBRID

        val code = v.toIntOrNull()
        if (code != null && code > 0) {
            if (code in 110..119 || code in 200..299) return FuelType.DIESEL
            if (code in 120..129 || code in 400..499) return FuelType.LPG
            if (code in 300..399) return FuelType.HYBRID
            if (code in 500..599) return FuelType.ELECTRIC
            if (code in 100..109) return FuelType.GASOLINE
        }
        return fallback
    }

    fun mapFuelTypeToFuelioCode(fuelType: FuelType): String = when (fuelType) {
        FuelType.DIESEL -> "110"
        FuelType.LPG -> "120"
        FuelType.HYBRID -> "301"
        FuelType.ELECTRIC -> "501"
        FuelType.GASOLINE -> "100"
        else -> "100"
    }

    private fun parseDateMillis(rawDate: String?): Long? {
        if (rawDate.isNullOrBlank()) return null
        val cleaned = rawDate.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (cleaned.isEmpty()) return null

        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy",
            "yyyy.MM.dd HH:mm",
            "yyyy.MM.dd",
            "MM/dd/yyyy"
        )
        for (pattern in patterns) {
            runCatching {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.isLenient = false
                if (pattern.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(cleaned)?.time?.let { return it }
            }
        }
        return null
    }

    private fun formatDateForFuelio(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return sdf.format(Date(epochMillis))
    }

    private fun parseNumber(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val normalized = raw.replace(Regex("\\s+"), "").replace(',', '.')
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
    }

    fun parseFuelioCsv(
        csvContent: String,
        vehicleId: String,
        defaultFuelType: FuelType = FuelType.GASOLINE
    ): FuelioImportResult {
        val cleanText = csvContent.removePrefix("\uFEFF")
        val rawLines = cleanText.split(Regex("\\r?\\n"))

        val fillUps = mutableListOf<FillUp>()
        val expenses = mutableListOf<Expense>()
        var vehicleFuelType: FuelType = defaultFuelType
        var maxOdometer = 0

        var currentSection = "none" // none, vehicle, log, costs, other
        var headers = listOf<String>()
        var delimiter = ','

        for (rawLine in rawLines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            val unquoted = trimmed.trim('"').trim()
            if (unquoted.startsWith("##")) {
                val secName = unquoted.replace(Regex("^#+\\s*"), "").lowercase(Locale.US)
                currentSection = when {
                    secName.startsWith("vehicle") -> "vehicle"
                    secName.startsWith("log") -> "log"
                    secName.startsWith("cost") && !secName.startsWith("costcategories") -> "costs"
                    else -> "other"
                }
                headers = emptyList()
                continue
            }

            if (currentSection == "other") continue

            if (headers.isEmpty()) {
                val commaCount = trimmed.count { it == ',' }
                val semiCount = trimmed.count { it == ';' }
                delimiter = if (semiCount > commaCount) ';' else ','

                val parsedHeaders = parseCsvLine(trimmed, delimiter).map { it.lowercase(Locale.US).trim() }
                if (currentSection == "none") {
                    val hasDate = parsedHeaders.any { it == "data" || it.contains("date") }
                    val hasOdo = parsedHeaders.any { it.contains("odo") || it.contains("kilom") }
                    val hasFuel = parsedHeaders.any { it.contains("fuel") || it.contains("volume") || it.contains("litre") }
                    if (hasDate && (hasOdo || hasFuel)) {
                        currentSection = "log"
                        headers = parsedHeaders
                        continue
                    }
                } else {
                    headers = parsedHeaders
                    continue
                }
            }

            val row = parseCsvLine(trimmed, delimiter)
            if (row.isEmpty() || (row.size == 1 && row[0].isEmpty())) continue

            fun getCol(vararg candidates: String): String? {
                for (cand in candidates) {
                    val exactIdx = headers.indexOfFirst { it == cand }
                    if (exactIdx != -1 && exactIdx < row.size) return row[exactIdx]
                }
                for (cand in candidates) {
                    val idx = headers.indexOfFirst { it.startsWith(cand) || it.contains(cand) }
                    if (idx != -1 && idx < row.size) return row[idx]
                }
                return null
            }

            when (currentSection) {
                "vehicle" -> {
                    val tankType = getCol("tank1type")
                    if (!tankType.isNullOrBlank()) {
                        vehicleFuelType = mapFuelioCodeToFuelType(tankType, defaultFuelType)
                    }
                }
                "log" -> {
                    val rawDate = getCol("data", "date") ?: row.getOrNull(0)
                    val dateMillis = parseDateMillis(rawDate) ?: continue

                    val odometer = parseNumber(getCol("odo", "odometer", "kilom") ?: row.getOrNull(1)).toInt()
                    val volume = parseNumber(getCol("fuel (", "fuel", "volume", "litre") ?: row.getOrNull(2))
                    if (odometer <= 0 && volume <= 0.0) continue

                    val rawFull = (getCol("full") ?: row.getOrNull(3) ?: "1").lowercase(Locale.US)
                    val isFullTank = rawFull == "1" || rawFull == "true" || rawFull == "yes" || rawFull == "oui"

                    val priceColIdx = headers.indexOfFirst {
                        it == "price (optional)" || it == "price" || it == "totalcost" || it == "total cost" || it == "cost"
                    }
                    val volPriceColIdx = headers.indexOfFirst {
                        it == "volumeprice" || it == "volume price" || it == "priceperunit" || it == "unitprice" || it == "prix/l"
                    }

                    val rawTotalOrPrice = if (priceColIdx != -1) parseNumber(row.getOrNull(priceColIdx)) else parseNumber(row.getOrNull(4))
                    val rawVolPrice = if (volPriceColIdx != -1) parseNumber(row.getOrNull(volPriceColIdx)) else parseNumber(row.getOrNull(13))

                    var totalCost = 0.0
                    var pricePerUnit = 0.0
                    if (rawTotalOrPrice > 0 && rawVolPrice > 0) {
                        totalCost = rawTotalOrPrice
                        pricePerUnit = rawVolPrice
                    } else if (rawTotalOrPrice > 0 && volume > 0) {
                        totalCost = rawTotalOrPrice
                        pricePerUnit = totalCost / volume
                    } else if (rawVolPrice > 0 && volume > 0) {
                        pricePerUnit = rawVolPrice
                        totalCost = volume * pricePerUnit
                    }

                    val station = (getCol("city", "station") ?: "").take(200)
                    val notes = (getCol("notes", "note") ?: "").take(5000)
                    val rawFuelType = getCol("fueltype", "fuel type")
                    val fuelType = mapFuelioCodeToFuelType(rawFuelType, vehicleFuelType)

                    if (odometer > maxOdometer) maxOdometer = odometer

                    fillUps.add(
                        FillUp(
                            id = "f-csv-${UUID.randomUUID().toString().take(8)}",
                            vehicleId = vehicleId,
                            date = dateMillis,
                            odometer = odometer,
                            volume = Math.round(volume * 100.0) / 100.0,
                            pricePerUnit = Math.round(pricePerUnit * 1000.0) / 1000.0,
                            totalCost = Math.round(totalCost * 100.0) / 100.0,
                            fuelType = fuelType,
                            isFullTank = isFullTank,
                            station = station,
                            notes = notes
                        )
                    )
                }
                "costs" -> {
                    val title = getCol("costtitle", "title", "description") ?: row.getOrNull(0) ?: "Fuelio Cost"
                    val rawDate = getCol("date", "data") ?: row.getOrNull(1)
                    val dateMillis = parseDateMillis(rawDate) ?: continue

                    val odometer = parseNumber(getCol("odo", "odometer") ?: row.getOrNull(2)).toInt()
                    val amount = parseNumber(getCol("cost", "amount") ?: row.getOrNull(5))
                    val notes = (getCol("notes", "note") ?: row.getOrNull(4) ?: "").take(5000)

                    if (amount <= 0.0) continue
                    if (odometer > maxOdometer) maxOdometer = odometer

                    expenses.add(
                        Expense(
                            id = "e-csv-${UUID.randomUUID().toString().take(8)}",
                            vehicleId = vehicleId,
                            date = dateMillis,
                            category = ExpenseCategory.OTHER,
                            amount = Math.round(amount * 100.0) / 100.0,
                            description = title.ifBlank { "Fuelio Cost" }.take(500),
                            odometer = odometer.takeIf { it > 0 },
                            notes = notes
                        )
                    )
                }
            }
        }

        return FuelioImportResult(
            fillUps = fillUps,
            expenses = expenses,
            maxOdometer = maxOdometer
        )
    }

    fun exportFuelioCsv(
        vehicle: Vehicle,
        fillUps: List<FillUp>,
        expenses: List<Expense> = emptyList(),
        units: Units = Units.METRIC
    ): String {
        val lines = mutableListOf<String>()
        val isMetric = units == Units.METRIC

        // 1. "## Vehicle" section
        lines.add("\"## Vehicle\"")
        lines.add(
            listOf(
                "Name",
                "Description",
                "DistUnit",
                "FuelUnit",
                "ConsumptionUnit",
                "ImportCSVDateFormat",
                "VIN",
                "Insurance",
                "Plate",
                "Make",
                "Model",
                "Year",
                "TankCount",
                "Tank1Type",
                "Tank2Type",
                "Active",
                "Tank1Capacity",
                "Tank2Capacity"
            ).joinToString(",") { escapeCsvCell(it) }
        )

        val unitCode = if (isMetric) "0" else "1"
        val tank1TypeCode = mapFuelTypeToFuelioCode(vehicle.fuelType)
        lines.add(
            listOf(
                vehicle.name.ifBlank { "${vehicle.make} ${vehicle.model}" },
                vehicle.notes,
                unitCode,
                unitCode,
                unitCode,
                "yyyy-MM-dd",
                vehicle.vin,
                "",
                vehicle.plate,
                vehicle.make,
                vehicle.model,
                vehicle.year,
                "1",
                tank1TypeCode,
                "0",
                "1",
                "55.0",
                "0.0"
            ).joinToString(",") { escapeCsvCell(it) }
        )

        // 2. "## Log" section
        lines.add("\"## Log\"")
        lines.add(
            listOf(
                "Data",
                if (isMetric) "Odo (km)" else "Odo (mi)",
                if (isMetric) "Fuel (litres)" else "Fuel (us gallons)",
                "Full",
                "Price (optional)",
                if (isMetric) "l/100km (optional)" else "mpg (optional)",
                "latitude (optional)",
                "longitude (optional)",
                "City (optional)",
                "Notes (optional)",
                "Missed",
                "TankNumber",
                "FuelType",
                "VolumePrice",
                "StationID (optional)",
                "ExcludeDistance",
                "UniqueId",
                "TankCalc"
            ).joinToString(",") { escapeCsvCell(it) }
        )

        val chronological = fillUps.sortedBy { it.odometer }
        val consumptionById = mutableMapOf<String, String>()
        for (i in 1 until chronological.size) {
            val prev = chronological[i - 1]
            val curr = chronological[i]
            val dist = curr.odometer - prev.odometer
            if (curr.isFullTank && prev.isFullTank && dist > 0 && curr.volume > 0) {
                val econ = if (isMetric) {
                    String.format(Locale.US, "%.2f", (curr.volume / dist) * 100.0)
                } else {
                    String.format(Locale.US, "%.2f", dist / curr.volume)
                }
                consumptionById[curr.id] = econ
            }
        }

        val sortedFillUps = fillUps.sortedByDescending { it.date }
        sortedFillUps.forEachIndexed { index, f ->
            val econ = consumptionById[f.id] ?: "0.0"
            lines.add(
                listOf(
                    formatDateForFuelio(f.date),
                    f.odometer,
                    String.format(Locale.US, "%.2f", f.volume),
                    if (f.isFullTank) "1" else "0",
                    String.format(Locale.US, "%.2f", f.totalCost),
                    econ,
                    "",
                    "",
                    f.station,
                    f.notes,
                    "0",
                    "1",
                    mapFuelTypeToFuelioCode(f.fuelType),
                    String.format(Locale.US, "%.3f", f.pricePerUnit),
                    "0",
                    "0",
                    index + 1,
                    "0.0"
                ).joinToString(",") { escapeCsvCell(it) }
            )
        }

        // 3. Optional "## Costs" section
        if (expenses.isNotEmpty()) {
            lines.add("\"## Costs\"")
            lines.add(
                listOf(
                    "CostTitle",
                    "Date",
                    "Odo",
                    "CostTypeID",
                    "Notes",
                    "Cost",
                    "RemindOdo",
                    "RemindDate",
                    "isTemplate",
                    "RepeatOdo",
                    "RepeatMonths",
                    "isIncome",
                    "UniqueId"
                ).joinToString(",") { escapeCsvCell(it) }
            )

            expenses.forEachIndexed { idx, e ->
                lines.add(
                    listOf(
                        e.description,
                        formatDateForFuelio(e.date),
                        e.odometer ?: 0,
                        "1",
                        e.notes,
                        String.format(Locale.US, "%.2f", e.amount),
                        "0",
                        "2000-01-01",
                        "0",
                        "0",
                        "0",
                        "0",
                        idx + 1
                    ).joinToString(",") { escapeCsvCell(it) }
                )
            }
        }

        return lines.joinToString("\r\n")
    }
}

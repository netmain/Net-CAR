package com.netcar.app.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.netcar.app.data.model.AppCurrency
import com.netcar.app.data.model.DistanceUnit
import com.netcar.app.data.model.ExpenseCategory
import com.netcar.app.data.model.FuelType
import com.netcar.app.data.model.ServiceType
import com.netcar.app.data.model.VolumeUnit
import java.util.Date

class Converters {
    private val gson = Gson()

    // Date
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // List<String>
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // FuelType
    @TypeConverter
    fun fromFuelType(value: FuelType?): String? = value?.name

    @TypeConverter
    fun toFuelType(value: String?): FuelType = value?.let {
        try { enumValueOf<FuelType>(it) } catch (e: Exception) { FuelType.GASOLINE }
    } ?: FuelType.GASOLINE

    // ExpenseCategory
    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory?): String? = value?.name

    @TypeConverter
    fun toExpenseCategory(value: String?): ExpenseCategory = value?.let {
        try { enumValueOf<ExpenseCategory>(it) } catch (e: Exception) { ExpenseCategory.OTHER }
    } ?: ExpenseCategory.OTHER

    // ServiceType
    @TypeConverter
    fun fromServiceType(value: ServiceType?): String? = value?.name

    @TypeConverter
    fun toServiceType(value: String?): ServiceType = value?.let {
        try { enumValueOf<ServiceType>(it) } catch (e: Exception) { ServiceType.OTHER }
    } ?: ServiceType.OTHER

    // DistanceUnit
    @TypeConverter
    fun fromDistanceUnit(value: DistanceUnit?): String? = value?.name

    @TypeConverter
    fun toDistanceUnit(value: String?): DistanceUnit = value?.let {
        try { enumValueOf<DistanceUnit>(it) } catch (e: Exception) { DistanceUnit.KM }
    } ?: DistanceUnit.KM

    // VolumeUnit
    @TypeConverter
    fun fromVolumeUnit(value: VolumeUnit?): String? = value?.name

    @TypeConverter
    fun toVolumeUnit(value: String?): VolumeUnit = value?.let {
        try { enumValueOf<VolumeUnit>(it) } catch (e: Exception) { VolumeUnit.LITERS }
    } ?: VolumeUnit.LITERS

    // AppCurrency
    @TypeConverter
    fun fromAppCurrency(value: AppCurrency?): String? = value?.name

    @TypeConverter
    fun toAppCurrency(value: String?): AppCurrency = value?.let {
        try { enumValueOf<AppCurrency>(it) } catch (e: Exception) { AppCurrency.EUR }
    } ?: AppCurrency.EUR
}

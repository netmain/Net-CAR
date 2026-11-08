package com.netcar.app.data.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val preferredLang: String = "en",
    val units: DistanceUnit = DistanceUnit.KM,
    val volumeUnit: VolumeUnit = VolumeUnit.LITERS,
    val currency: AppCurrency = AppCurrency.EUR
)

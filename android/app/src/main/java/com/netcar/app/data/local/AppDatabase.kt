package com.netcar.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.netcar.app.data.local.dao.ExpenseDao
import com.netcar.app.data.local.dao.FillUpDao
import com.netcar.app.data.local.dao.ReminderDao
import com.netcar.app.data.local.dao.ServiceDao
import com.netcar.app.data.local.dao.VehicleDao
import com.netcar.app.data.local.entity.ExpenseEntity
import com.netcar.app.data.local.entity.FillUpEntity
import com.netcar.app.data.local.entity.ReminderEntity
import com.netcar.app.data.local.entity.ServiceEntity
import com.netcar.app.data.local.entity.VehicleEntity

@Database(
    entities = [
        VehicleEntity::class,
        FillUpEntity::class,
        ExpenseEntity::class,
        ServiceEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun fillUpDao(): FillUpDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun serviceDao(): ServiceDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "netcar_db"
    }
}

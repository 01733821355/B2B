package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AppSettingDao
import com.example.data.dao.AuditLogDao
import com.example.data.dao.CustomerFileDao
import com.example.data.dao.FileAttachmentDao
import com.example.data.dao.RmTargetDao
import com.example.data.dao.SmsNotificationDao
import com.example.data.dao.UserDao
import com.example.data.dao.UserLocationLogDao
import com.example.data.model.AppSettingEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.CustomerFileEntity
import com.example.data.model.FileAttachmentEntity
import com.example.data.model.RmTargetEntity
import com.example.data.model.SmsNotificationEntity
import com.example.data.model.SyncStatusEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserLocationLogEntity

@Database(
  entities = [
    UserEntity::class,
    CustomerFileEntity::class,
    FileAttachmentEntity::class,
    AuditLogEntity::class,
    AppSettingEntity::class,
    SyncStatusEntity::class,
    UserLocationLogEntity::class,
    RmTargetEntity::class,
    SmsNotificationEntity::class
  ],
  version = 5,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun customerFileDao(): CustomerFileDao
  abstract fun fileAttachmentDao(): FileAttachmentDao
  abstract fun auditLogDao(): AuditLogDao
  abstract fun appSettingDao(): AppSettingDao
  abstract fun userLocationLogDao(): UserLocationLogDao
  abstract fun rmTargetDao(): RmTargetDao
  abstract fun smsNotificationDao(): SmsNotificationDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "ebl_rm_database.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}

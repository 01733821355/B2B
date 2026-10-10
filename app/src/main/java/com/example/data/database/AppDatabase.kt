package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppSettingDao
import com.example.data.dao.AuditLogDao
import com.example.data.dao.CommunicationDao
import com.example.data.dao.CustomerFileDao
import com.example.data.dao.FileAttachmentDao
import com.example.data.dao.ImportantDocumentDao
import com.example.data.dao.RmTargetDao
import com.example.data.dao.SmsNotificationDao
import com.example.data.dao.UserDao
import com.example.data.dao.UserLocationLogDao
import com.example.data.model.AppSettingEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CustomerFileEntity
import com.example.data.model.EventResponseEntity
import com.example.data.model.FileAttachmentEntity
import com.example.data.model.ImportantDocumentEntity
import com.example.data.model.RmTargetEntity
import com.example.data.model.SmsNotificationEntity
import com.example.data.model.SyncStatusEntity
import com.example.data.model.TeamEventEntity
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
    SmsNotificationEntity::class,
    ChatMessageEntity::class,
    TeamEventEntity::class,
    EventResponseEntity::class,
    ImportantDocumentEntity::class
  ],
  version = 9,
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
  abstract fun communicationDao(): CommunicationDao
  abstract fun importantDocumentDao(): ImportantDocumentDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `chat_messages` (
            `id` TEXT NOT NULL,
            `senderRmCode` TEXT NOT NULL,
            `senderName` TEXT NOT NULL,
            `senderRole` TEXT NOT NULL,
            `recipientRmCode` TEXT,
            `messageText` TEXT NOT NULL,
            `timestamp` INTEGER NOT NULL,
            `messageType` TEXT NOT NULL,
            `eventId` TEXT,
            PRIMARY KEY(`id`)
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_timestamp` ON `chat_messages` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_recipientRmCode` ON `chat_messages` (`recipientRmCode`)")

        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `team_events` (
            `eventId` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `description` TEXT NOT NULL,
            `creatorRmCode` TEXT NOT NULL,
            `creatorName` TEXT NOT NULL,
            `targetDate` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `status` TEXT NOT NULL,
            PRIMARY KEY(`eventId`)
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_events_createdAt` ON `team_events` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_events_status` ON `team_events` (`status`)")

        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `event_responses` (
            `responseId` TEXT NOT NULL,
            `eventId` TEXT NOT NULL,
            `rmCode` TEXT NOT NULL,
            `rmName` TEXT NOT NULL,
            `filesCount` INTEGER NOT NULL,
            `requestedDate` TEXT NOT NULL,
            `location` TEXT NOT NULL,
            `remarks` TEXT NOT NULL,
            `submittedAt` INTEGER NOT NULL,
            PRIMARY KEY(`responseId`)
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_responses_eventId` ON `event_responses` (`eventId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_responses_rmCode` ON `event_responses` (`rmCode`)")
      }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `customer_files` ADD COLUMN `serialNumber` TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_files_mobile` ON `customer_files` (`mobile`)")
      }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `important_documents` (
            `docId` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `description` TEXT NOT NULL,
            `fileName` TEXT NOT NULL,
            `fileType` TEXT NOT NULL,
            `fileSizeBytes` INTEGER NOT NULL,
            `fileUri` TEXT NOT NULL,
            `storagePath` TEXT NOT NULL,
            `uploadedBy` TEXT NOT NULL,
            `uploaderName` TEXT NOT NULL,
            `uploaderRole` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            `isDeleted` INTEGER NOT NULL,
            `isSynced` INTEGER NOT NULL,
            PRIMARY KEY(`docId`)
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_important_documents_category` ON `important_documents` (`category`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_important_documents_createdAt` ON `important_documents` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_important_documents_isDeleted` ON `important_documents` (`isDeleted`)")
      }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `team_events` ADD COLUMN `allowedFields` TEXT NOT NULL DEFAULT 'CUSTOMERS,COUNT,DATE,LOCATION,REMARKS'")
        db.execSQL("ALTER TABLE `event_responses` ADD COLUMN `customerEntriesJson` TEXT NOT NULL DEFAULT ''")
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "ebl_rm_database.db"
        )
          .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}

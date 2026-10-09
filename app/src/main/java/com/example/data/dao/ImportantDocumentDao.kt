package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ImportantDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportantDocumentDao {
  @Query("SELECT * FROM important_documents WHERE isDeleted = 0 ORDER BY updatedAt DESC")
  fun getAllDocumentsFlow(): Flow<List<ImportantDocumentEntity>>

  @Query("SELECT * FROM important_documents WHERE isDeleted = 0 ORDER BY updatedAt DESC")
  suspend fun getAllActiveDocuments(): List<ImportantDocumentEntity>

  @Query("SELECT * FROM important_documents WHERE docId = :id LIMIT 1")
  suspend fun getDocumentById(id: String): ImportantDocumentEntity?

  @Query("SELECT * FROM important_documents WHERE isDeleted = 0 AND category = :category ORDER BY updatedAt DESC")
  fun getDocumentsByCategoryFlow(category: String): Flow<List<ImportantDocumentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDocument(doc: ImportantDocumentEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDocuments(docs: List<ImportantDocumentEntity>)

  @Update
  suspend fun updateDocument(doc: ImportantDocumentEntity)

  @Query("UPDATE important_documents SET isDeleted = 1, updatedAt = :updatedAt, isSynced = 0 WHERE docId = :id")
  suspend fun softDeleteDocument(id: String, updatedAt: Long)

  @Query("DELETE FROM important_documents WHERE docId = :id")
  suspend fun permanentDeleteDocument(id: String)
}

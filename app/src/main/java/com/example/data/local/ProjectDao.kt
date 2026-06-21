package com.example.data.local

import androidx.room.*
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CommitEntity
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT * FROM commits WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getCommitsForProject(projectId: Long): Flow<List<CommitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommit(commit: CommitEntity)

    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getChatMessagesForProject(projectId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE projectId = :projectId")
    suspend fun clearChatMessagesForProject(projectId: Long)
}

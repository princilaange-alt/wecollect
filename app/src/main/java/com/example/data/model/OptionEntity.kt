package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table OPTIONS (Schéma Supabase / PostgreSQL)
 */
@Entity(
    tableName = "options",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id_question"],
            childColumns = ["id_question"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["id_question"]),
        Index(value = ["id_question", "ordre"])
    ]
)
data class OptionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_option")
    val idOption: Long = 0,
    @ColumnInfo(name = "id_question")
    val idQuestion: Long,
    val libelle: String,
    val ordre: Int = 1,
    @ColumnInfo(name = "created_at")
    val createdAt: String = System.currentTimeMillis().toString()
)


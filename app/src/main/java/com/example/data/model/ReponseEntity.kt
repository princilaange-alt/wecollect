package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table REPONSES (Schéma Supabase / PostgreSQL)
 */
@Entity(
    tableName = "reponses",
    foreignKeys = [
        ForeignKey(
            entity = EnqueteEntity::class,
            parentColumns = ["id_enquete"],
            childColumns = ["id_enquete"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id_question"],
            childColumns = ["id_question"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OptionEntity::class,
            parentColumns = ["id_option"],
            childColumns = ["id_option"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["id_enquete", "id_question"], unique = true),
        Index(value = ["id_enquete"]),
        Index(value = ["id_question"]),
        Index(value = ["id_option"])
    ]
)
data class ReponseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_reponse")
    val idReponse: Long = 0,
    @ColumnInfo(name = "id_enquete")
    val idEnquete: Long,
    @ColumnInfo(name = "id_question")
    val idQuestion: Long,
    @ColumnInfo(name = "id_option")
    val idOption: Long? = null,
    val valeur: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String = System.currentTimeMillis().toString()
)


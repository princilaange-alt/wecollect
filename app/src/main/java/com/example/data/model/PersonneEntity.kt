package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table PERSONNES (Schéma Supabase / PostgreSQL)
 * Contient seulement l'identité stable (code_unique).
 * Les informations comme nom, prénom, sexe, etc. sont stockées dans REPONSES.
 */
@Entity(
    tableName = "personnes",
    indices = [
        Index(value = ["code_unique"], unique = true)
    ]
)
data class PersonneEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_personne")
    val idPersonne: Long = 0,
    @ColumnInfo(name = "code_unique")
    val codeUnique: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String = System.currentTimeMillis().toString(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = System.currentTimeMillis().toString()
)

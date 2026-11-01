package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table USERS (Schéma Supabase / PostgreSQL)
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["role"]),
        Index(value = ["statut"]),
        Index(value = ["matricule"]),
        Index(value = ["region"])
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_user")
    val idUser: Long = 0,
    val nom: String,
    val prenom: String,
    val email: String,
    val telephone: String? = null,
    @ColumnInfo(name = "mot_de_passe")
    val motDePasse: String? = null,
    val role: String = "ENQUETEUR", // ADMIN, ENQUETEUR
    val statut: String = "ACTIF", // ACTIF, INACTIF
    @ColumnInfo(name = "region", defaultValue = "Analamanga")
    val region: String = "Analamanga",
    @ColumnInfo(name = "matricule")
    val matricule: String? = null,
    @ColumnInfo(name = "photo")
    val photo: String? = null,
    @ColumnInfo(name = "photo_url")
    val photoUrl: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String = System.currentTimeMillis().toString(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = System.currentTimeMillis().toString()
)


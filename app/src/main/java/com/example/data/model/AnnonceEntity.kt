package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "annonces")
data class AnnonceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_annonce")
    val idAnnonce: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "titre")
    val titre: String,

    @ColumnInfo(name = "contenu")
    val contenu: String? = null,

    @ColumnInfo(name = "type_annonce")
    val typeAnnonce: String = "TEXTE", // TEXTE, FICHIER, MIXTE

    @ColumnInfo(name = "priorite")
    val priorite: String = "NORMALE", // NORMALE, IMPORTANTE, URGENTE

    @ColumnInfo(name = "statut")
    val statut: String = "PUBLIEE", // BROUILLON, PUBLIEE, ARCHIVEE

    @ColumnInfo(name = "cible_type")
    val cibleType: String = "TOUS", // TOUS, REGION, INDIVIDUELS

    @ColumnInfo(name = "cible_valeur")
    val cibleValeur: String? = null,

    @ColumnInfo(name = "date_publication")
    val datePublication: String? = null,

    @ColumnInfo(name = "date_expiration")
    val dateExpiration: String? = null,

    @ColumnInfo(name = "created_by")
    val createdBy: Long? = null,

    @ColumnInfo(name = "author_name")
    val authorName: String? = null,

    @ColumnInfo(name = "author_role")
    val authorRole: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: String = System.currentTimeMillis().toString(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: String = System.currentTimeMillis().toString()
)

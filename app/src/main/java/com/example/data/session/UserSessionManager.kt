package com.example.data.session

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserEntity

/**
 * Mitahiry ny mombamomba ny mpampiasa mavitrika eo an-toerana (Local Session Storage).
 * Izany dia ahafahan'ny mpanadihady miditra sy mampiasa ny fampiharana
 * na dia tapaka aza ny Internet (Accès hors-ligne / Zone blanche), ary mitazona
 * ny fidirana na dia vonoina na averina akatona aza ny fampiharana.
 */
class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveSession(user: UserEntity) {
        if (com.example.data.sync.SyncService.isInactiveStatut(user.statut)) {
            clearSession()
            updateSavedAccountStatut(user.idUser, user.email, user.statut)
            return
        }
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, user.idUser)
            putString(KEY_NOM, user.nom)
            putString(KEY_PRENOM, user.prenom)
            putString(KEY_EMAIL, user.email)
            putString(KEY_TELEPHONE, user.telephone ?: "")
            putString(KEY_ROLE, user.role)
            putString(KEY_STATUT, user.statut)
            putString(KEY_REGION, user.region)
            putString(KEY_MATRICULE, user.matricule ?: "")
            putString(KEY_MOT_DE_PASSE, user.motDePasse ?: "")
            putString(KEY_PHOTO, user.photo ?: "")
            putString(KEY_PHOTO_URL, user.photoUrl ?: "")
            apply()
        }
        saveAccount(user)
    }

    fun getSession(): UserEntity? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val id = prefs.getLong(KEY_USER_ID, 0L)
        val nom = prefs.getString(KEY_NOM, "") ?: ""
        val prenom = prefs.getString(KEY_PRENOM, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val telephone = prefs.getString(KEY_TELEPHONE, null)
        val role = prefs.getString(KEY_ROLE, "ENQUETEUR") ?: "ENQUETEUR"
        val statut = prefs.getString(KEY_STATUT, "ACTIF") ?: "ACTIF"
        val region = prefs.getString(KEY_REGION, "Analamanga") ?: "Analamanga"
        val matricule = prefs.getString(KEY_MATRICULE, null)?.takeIf { it.isNotBlank() }
        if (com.example.data.sync.SyncService.isInactiveStatut(statut)) {
            clearSession()
            return null
        }

        val motDePasse = prefs.getString(KEY_MOT_DE_PASSE, null)
        val rawPhoto = prefs.getString(KEY_PHOTO, null)?.takeIf { it.isNotBlank() }
        val rawPhotoUrl = prefs.getString(KEY_PHOTO_URL, null)?.takeIf { it.isNotBlank() }
        val photo = rawPhoto ?: rawPhotoUrl ?: when (id) {
            2L -> com.example.data.util.RealUserPhotos.USER_2_PHOTO
            3L -> com.example.data.util.RealUserPhotos.USER_3_PHOTO
            4L -> com.example.data.util.RealUserPhotos.USER_4_PHOTO
            else -> com.example.data.util.RealUserPhotos.USER_1_PHOTO
        }
        val photoUrl = rawPhotoUrl ?: photo

        if (id == 0L && email.isEmpty() && nom.isEmpty()) {
            return null
        }

        return UserEntity(
            idUser = id,
            nom = nom,
            prenom = prenom,
            email = email,
            telephone = if (telephone.isNullOrBlank()) null else telephone,
            motDePasse = motDePasse,
            role = role,
            statut = statut,
            region = region,
            matricule = matricule,
            photo = photo,
            photoUrl = photoUrl
        )
    }

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_NOM)
            remove(KEY_PRENOM)
            remove(KEY_EMAIL)
            remove(KEY_TELEPHONE)
            remove(KEY_ROLE)
            remove(KEY_STATUT)
            remove(KEY_REGION)
            remove(KEY_MATRICULE)
            remove(KEY_MOT_DE_PASSE)
            remove(KEY_PHOTO)
            remove(KEY_PHOTO_URL)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    /**
     * Maka ny lisitry ny kaonty rehetra efa tafiditra (Saved / Remembered Accounts).
     */
    fun getSavedAccounts(): List<UserEntity> {
        val jsonStr = prefs.getString(KEY_SAVED_ACCOUNTS, null)
        val list = mutableListOf<UserEntity>()
        if (jsonStr.isNullOrBlank()) {
            val defaults = listOf(
                UserEntity(
                    idUser = 2,
                    nom = "RAKOTO",
                    prenom = "Jean",
                    email = "jean.rakoto@wecollect.mg",
                    telephone = "0341111111",
                    motDePasse = "Enqueteur@123",
                    role = "ENQUETEUR",
                    statut = "ACTIF",
                    region = "Analamanga",
                    matricule = "ENQ-002",
                    photo = com.example.data.util.RealUserPhotos.USER_2_PHOTO,
                    photoUrl = com.example.data.util.RealUserPhotos.USER_2_PHOTO
                ),
                UserEntity(
                    idUser = 1,
                    nom = "RAHARINIRINA",
                    prenom = "Juvance",
                    email = "juvanceraharinirina@gmail.com",
                    telephone = "0345812888",
                    motDePasse = "261103",
                    role = "ADMIN",
                    statut = "ACTIF",
                    region = "Analamanga",
                    matricule = "ADM-001",
                    photo = com.example.data.util.RealUserPhotos.USER_1_PHOTO,
                    photoUrl = com.example.data.util.RealUserPhotos.USER_1_PHOTO
                ),
                UserEntity(
                    idUser = 3,
                    nom = "RABE",
                    prenom = "Paul",
                    email = "paulrabe@gmail.com",
                    telephone = "0342222222",
                    motDePasse = "261103",
                    role = "ENQUETEUR",
                    statut = "ACTIF",
                    region = "Vakinankaratra",
                    matricule = "ENQ-003",
                    photo = com.example.data.util.RealUserPhotos.USER_3_PHOTO,
                    photoUrl = com.example.data.util.RealUserPhotos.USER_3_PHOTO
                ),
                UserEntity(
                    idUser = 4,
                    nom = "RASOLO",
                    prenom = "Marie",
                    email = "marie@gmail.com",
                    telephone = "0343333333",
                    motDePasse = "261103",
                    role = "ENQUETEUR",
                    statut = "ACTIF",
                    region = "Atsinanana",
                    matricule = "ENQ-004",
                    photo = com.example.data.util.RealUserPhotos.USER_4_PHOTO,
                    photoUrl = com.example.data.util.RealUserPhotos.USER_4_PHOTO
                )
            )
            writeSavedAccounts(defaults)
            return defaults
        }
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optLong("id_user", 0L)
                val nom = obj.optString("nom", "")
                val prenom = obj.optString("prenom", "")
                val email = obj.optString("email", "")
                val tel = if (obj.isNull("telephone") || obj.optString("telephone").isBlank()) null else obj.optString("telephone")
                val pass = if (obj.isNull("mot_de_passe") || obj.optString("mot_de_passe").isBlank()) null else obj.optString("mot_de_passe")
                val role = obj.optString("role", "ENQUETEUR")
                val statut = obj.optString("statut", "ACTIF")
                val region = if (obj.isNull("region") || obj.optString("region").isBlank()) "Analamanga" else obj.optString("region")
                val matricule = if (obj.isNull("matricule") || obj.optString("matricule").isBlank()) null else obj.optString("matricule")
                val created = obj.optString("created_at", "")
                val updated = obj.optString("updated_at", "")
                val rawP = if (obj.isNull("photo") || obj.optString("photo").isBlank()) null else obj.optString("photo")
                val rawPu = if (obj.isNull("photo_url") || obj.optString("photo_url").isBlank()) null else obj.optString("photo_url")
                val photo = rawP ?: rawPu ?: when (id) {
                    2L -> com.example.data.util.RealUserPhotos.USER_2_PHOTO
                    3L -> com.example.data.util.RealUserPhotos.USER_3_PHOTO
                    4L -> com.example.data.util.RealUserPhotos.USER_4_PHOTO
                    else -> com.example.data.util.RealUserPhotos.USER_1_PHOTO
                }
                val photoUrl = rawPu ?: photo

                list.add(
                    UserEntity(
                        idUser = id,
                        nom = nom,
                        prenom = prenom,
                        email = email,
                        telephone = tel,
                        motDePasse = pass,
                        role = role,
                        statut = statut,
                        region = region,
                        matricule = matricule,
                        photo = photo,
                        photoUrl = photoUrl,
                        createdAt = created,
                        updatedAt = updated
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Mitahiry kaonty tafiditra ao amin'ny lisitry ny kaonty voatahiry (Saved Accounts).
     */
    fun saveAccount(user: UserEntity) {
        val list = getSavedAccounts().toMutableList()
        list.removeAll {
            (user.email.isNotBlank() && it.email.isNotBlank() && it.email.trim().equals(user.email.trim(), ignoreCase = true)) ||
            (!user.telephone.isNullOrBlank() && !it.telephone.isNullOrBlank() && it.telephone!!.trim() == user.telephone!!.trim()) ||
            (!user.matricule.isNullOrBlank() && !it.matricule.isNullOrBlank() && it.matricule!!.trim().equals(user.matricule!!.trim(), ignoreCase = true)) ||
            (user.idUser > 0L && it.idUser > 0L && it.idUser == user.idUser) ||
            (user.nom.isNotBlank() && user.prenom.isNotBlank() && it.nom.trim().equals(user.nom.trim(), ignoreCase = true) && it.prenom.trim().equals(user.prenom.trim(), ignoreCase = true))
        }
        list.add(0, user)
        writeSavedAccounts(list)
    }

    /**
     * Mamafa kaonty iray ao amin'ny lisitry ny kaonty voatahiry (Supprimer de cet appareil).
     */
    fun removeSavedAccount(userId: Long, email: String) {
        val list = getSavedAccounts().toMutableList()
        list.removeAll {
            (email.isNotBlank() && it.email.isNotBlank() && it.email.trim().equals(email.trim(), ignoreCase = true)) ||
            (userId > 0L && it.idUser > 0L && it.idUser == userId)
        }
        writeSavedAccounts(list)
    }

    /**
     * Manavao ny satan'ny kaonty ao amin'ny lisitra (ACTIF <-> INACTIF).
     */
    fun updateSavedAccountStatut(userId: Long, email: String, newStatut: String, telephone: String? = null, nom: String? = null, prenom: String? = null) {
        val list = getSavedAccounts().toMutableList()
        val index = list.indexOfFirst {
            (email.isNotBlank() && it.email.isNotBlank() && it.email.trim().equals(email.trim(), ignoreCase = true)) ||
            (!telephone.isNullOrBlank() && !it.telephone.isNullOrBlank() && it.telephone!!.trim() == telephone.trim()) ||
            (userId > 0L && it.idUser > 0L && it.idUser == userId) ||
            (!nom.isNullOrBlank() && !prenom.isNullOrBlank() && it.nom.trim().equals(nom.trim(), ignoreCase = true) && it.prenom.trim().equals(prenom.trim(), ignoreCase = true))
        }
        if (index != -1) {
            val u = list[index]
            list[index] = u.copy(
                statut = newStatut,
                nom = if (!nom.isNullOrBlank()) nom else u.nom,
                prenom = if (!prenom.isNullOrBlank()) prenom else u.prenom
            )
            writeSavedAccounts(list)
        }
    }

    private fun writeSavedAccounts(list: List<UserEntity>) {
        val array = org.json.JSONArray()
        for (u in list) {
            val obj = org.json.JSONObject().apply {
                put("id_user", u.idUser)
                put("nom", u.nom)
                put("prenom", u.prenom)
                put("email", u.email)
                put("telephone", u.telephone ?: "")
                put("mot_de_passe", u.motDePasse ?: "")
                put("role", u.role)
                put("statut", u.statut)
                put("region", u.region)
                put("matricule", u.matricule ?: "")
                put("photo", u.photo ?: "")
                put("photo_url", u.photoUrl ?: "")
                put("created_at", u.createdAt)
                put("updated_at", u.updatedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_ACCOUNTS, array.toString()).apply()
    }

    fun addPendingDeletion(idEnquete: Long) {
        val current = getPendingDeletions().toMutableSet()
        current.add(idEnquete)
        prefs.edit().putStringSet(KEY_PENDING_DELETIONS, current.map { it.toString() }.toSet()).apply()
    }

    fun getPendingDeletions(): Set<Long> {
        val set = prefs.getStringSet(KEY_PENDING_DELETIONS, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun removePendingDeletion(idEnquete: Long) {
        val current = getPendingDeletions().toMutableSet()
        current.remove(idEnquete)
        prefs.edit().putStringSet(KEY_PENDING_DELETIONS, current.map { it.toString() }.toSet()).apply()
    }

    fun setRememberMe(remember: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, remember).apply()
        if (!remember) {
            prefs.edit().remove(KEY_LAST_IDENTIFIER).apply()
        }
    }

    fun isRememberMe(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME, true)
    }

    fun setLastIdentifier(identifier: String) {
        if (isRememberMe()) {
            prefs.edit().putString(KEY_LAST_IDENTIFIER, identifier).apply()
        }
    }

    fun getLastIdentifier(): String {
        return if (isRememberMe()) {
            prefs.getString(KEY_LAST_IDENTIFIER, "") ?: ""
        } else {
            ""
        }
    }

    companion object {
        private const val PREF_NAME = "enquete_user_session_pref"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NOM = "user_nom"
        private const val KEY_PRENOM = "user_prenom"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_TELEPHONE = "user_telephone"
        private const val KEY_ROLE = "user_role"
        private const val KEY_STATUT = "user_statut"
        private const val KEY_REGION = "user_region"
        private const val KEY_MATRICULE = "user_matricule"
        private const val KEY_MOT_DE_PASSE = "user_mot_de_passe"
        private const val KEY_PHOTO = "user_photo"
        private const val KEY_PHOTO_URL = "user_photo_url"
        private const val KEY_SAVED_ACCOUNTS = "saved_logged_in_accounts"
        private const val KEY_PENDING_DELETIONS = "pending_remote_deletions"
        private const val KEY_REMEMBER_ME = "remember_me_pref"
        private const val KEY_LAST_IDENTIFIER = "last_identifier_pref"
    }
}

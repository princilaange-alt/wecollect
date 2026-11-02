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

    /**
     * Mitahiry an-toerana ny valintenin'ny questionnaire eo am-pamenoana (Auto-save draft).
     * Miantoka fa na miovaova aza ny connexion na tapaka tampoka, tsy very mihitsy ny nosoratan'ny mpanadihady.
     */
    fun saveFormDraft(
        questionnaireId: Long,
        codeUnique: String,
        dateEnquete: String,
        textResponses: Map<Long, String>,
        selectedOptions: Map<Long, List<Long>>,
        autreResponses: Map<Long, String>
    ) {
        try {
            val root = org.json.JSONObject().apply {
                put("questionnaire_id", questionnaireId)
                put("code_unique", codeUnique)
                put("date_enquete", dateEnquete)

                val textObj = org.json.JSONObject()
                for ((k, v) in textResponses) {
                    textObj.put(k.toString(), v)
                }
                put("text_responses", textObj)

                val optObj = org.json.JSONObject()
                for ((k, v) in selectedOptions) {
                    val arr = org.json.JSONArray()
                    v.forEach { arr.put(it) }
                    optObj.put(k.toString(), arr)
                }
                put("selected_options", optObj)

                val autreObj = org.json.JSONObject()
                for ((k, v) in autreResponses) {
                    autreObj.put(k.toString(), v)
                }
                put("autre_responses", autreObj)
                put("timestamp", System.currentTimeMillis())
            }
            prefs.edit().putString(KEY_FORM_DRAFT_PREFIX + questionnaireId, root.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getFormDraft(questionnaireId: Long): FormDraftData? {
        val str = prefs.getString(KEY_FORM_DRAFT_PREFIX + questionnaireId, null) ?: return null
        return try {
            val root = org.json.JSONObject(str)
            val code = root.optString("code_unique", "")
            val date = root.optString("date_enquete", "")

            val textMap = mutableMapOf<Long, String>()
            val textObj = root.optJSONObject("text_responses")
            if (textObj != null) {
                val it = textObj.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    k.toLongOrNull()?.let { id -> textMap[id] = textObj.optString(k, "") }
                }
            }

            val optMap = mutableMapOf<Long, List<Long>>()
            val optObj = root.optJSONObject("selected_options")
            if (optObj != null) {
                val it = optObj.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    k.toLongOrNull()?.let { id ->
                        val arr = optObj.optJSONArray(k)
                        if (arr != null) {
                            val list = mutableListOf<Long>()
                            for (i in 0 until arr.length()) {
                                list.add(arr.getLong(i))
                            }
                            optMap[id] = list
                        }
                    }
                }
            }

            val autreMap = mutableMapOf<Long, String>()
            val autreObj = root.optJSONObject("autre_responses")
            if (autreObj != null) {
                val it = autreObj.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    k.toLongOrNull()?.let { id -> autreMap[id] = autreObj.optString(k, "") }
                }
            }

            FormDraftData(
                questionnaireId = questionnaireId,
                codeUnique = code,
                dateEnquete = date,
                textResponses = textMap,
                selectedOptions = optMap,
                autreResponses = autreMap
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearFormDraft(questionnaireId: Long) {
        prefs.edit().remove(KEY_FORM_DRAFT_PREFIX + questionnaireId).apply()
    }

    /**
     * Mitahiry ny safidy ny questionnaire (Selectionner questionnaire) ao amin'ny fitahirizana an-toerana (Local)
     * mba tsy ho very na hovaina na dia tapaka ny connexion na akatona ny fampiharana.
     */
    fun saveSelectedQuestionnaireId(id: Long) {
        prefs.edit().putLong(KEY_SELECTED_QUESTIONNAIRE_ID, id).apply()
    }

    fun getSelectedQuestionnaireId(): Long? {
        if (!prefs.contains(KEY_SELECTED_QUESTIONNAIRE_ID)) return null
        val id = prefs.getLong(KEY_SELECTED_QUESTIONNAIRE_ID, -1L)
        return if (id > 0L) id else null
    }

    fun clearSelectedQuestionnaireId() {
        prefs.edit().remove(KEY_SELECTED_QUESTIONNAIRE_ID).apply()
    }

    /**
     * Mitahiry ireo annonces noforonina an-toerana nefa mbola miandry fandefasana any amin'ny Supabase
     * rehefa miverina ny connexion.
     */
    fun addPendingAnnonceSync(idAnnonce: String) {
        val current = getPendingAnnonceSyncIds().toMutableSet()
        current.add(idAnnonce)
        prefs.edit().putStringSet(KEY_PENDING_ANNONCE_SYNC, current).apply()
    }

    fun getPendingAnnonceSyncIds(): Set<String> {
        return prefs.getStringSet(KEY_PENDING_ANNONCE_SYNC, emptySet()) ?: emptySet()
    }

    fun removePendingAnnonceSync(idAnnonce: String) {
        val current = getPendingAnnonceSyncIds().toMutableSet()
        current.remove(idAnnonce)
        prefs.edit().putStringSet(KEY_PENDING_ANNONCE_SYNC, current).apply()
    }

    /**
     * Mitahiry ireo fanamarihana "novakiana" (LU) natao tamin'ny offline
     */
    fun addPendingAnnonceRead(idAnnonce: String, userId: Long) {
        val key = "${idAnnonce}_${userId}"
        val current = getPendingAnnonceReads().toMutableSet()
        current.add(key)
        prefs.edit().putStringSet(KEY_PENDING_ANNONCE_READS, current).apply()
    }

    fun getPendingAnnonceReads(): Set<String> {
        return prefs.getStringSet(KEY_PENDING_ANNONCE_READS, emptySet()) ?: emptySet()
    }

    fun removePendingAnnonceRead(idAnnonce: String, userId: Long) {
        val key = "${idAnnonce}_${userId}"
        val current = getPendingAnnonceReads().toMutableSet()
        current.remove(key)
        prefs.edit().putStringSet(KEY_PENDING_ANNONCE_READS, current).apply()
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
        private const val KEY_FORM_DRAFT_PREFIX = "active_form_draft_q_"
        private const val KEY_SELECTED_QUESTIONNAIRE_ID = "selected_questionnaire_id_pref"
        private const val KEY_PENDING_ANNONCE_SYNC = "pending_annonce_sync_ids"
        private const val KEY_PENDING_ANNONCE_READS = "pending_annonce_read_keys"
    }
}

data class FormDraftData(
    val questionnaireId: Long,
    val codeUnique: String,
    val dateEnquete: String,
    val textResponses: Map<Long, String>,
    val selectedOptions: Map<Long, List<Long>>,
    val autreResponses: Map<Long, String>
)

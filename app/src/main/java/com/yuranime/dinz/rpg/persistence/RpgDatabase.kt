package com.yuranime.dinz.rpg.persistence

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.yuranime.dinz.data.firebase.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
interface RpgSaveDao {
    @Query("SELECT * FROM rpg_player_state WHERE userUid = :userUid")
    suspend fun getPlayerState(userUid: String): PlayerStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerState(state: PlayerStateEntity)

    @Query("SELECT * FROM rpg_inventory WHERE userUid = :userUid")
    suspend fun getInventory(userUid: String): List<InventoryItemEntity>

    @Query("DELETE FROM rpg_inventory WHERE userUid = :userUid")
    suspend fun clearInventory(userUid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(items: List<InventoryItemEntity>)

    @Query("SELECT * FROM rpg_fish_collection WHERE userUid = :userUid")
    suspend fun getFishCollection(userUid: String): List<FishCollectionEntity>

    @Query("DELETE FROM rpg_fish_collection WHERE userUid = :userUid")
    suspend fun clearFishCollection(userUid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFishCollection(items: List<FishCollectionEntity>)

    @Query("SELECT * FROM rpg_trees WHERE userUid = :userUid")
    suspend fun getTrees(userUid: String): List<TreeStateEntity>

    @Query("DELETE FROM rpg_trees WHERE userUid = :userUid")
    suspend fun clearTrees(userUid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrees(trees: List<TreeStateEntity>)

    @Query("SELECT * FROM rpg_crops WHERE userUid = :userUid")
    suspend fun getCrops(userUid: String): List<CropPlotEntity>

    @Query("DELETE FROM rpg_crops WHERE userUid = :userUid")
    suspend fun clearCrops(userUid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrops(crops: List<CropPlotEntity>)

    @Query("SELECT * FROM rpg_placed_objects WHERE userUid = :userUid")
    suspend fun getPlacedObjects(userUid: String): List<PlacedObjectEntity>

    @Query("DELETE FROM rpg_placed_objects WHERE userUid = :userUid")
    suspend fun clearPlacedObjects(userUid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlacedObjects(objects: List<PlacedObjectEntity>)

    @Query("SELECT * FROM rpg_quests WHERE userUid = :userUid")
    suspend fun getQuests(userUid: String): List<QuestProgressEntity>

    @Query("DELETE FROM rpg_quests WHERE userUid = :userUid")
    suspend fun clearQuests(userUid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<QuestProgressEntity>)

    @Query("SELECT * FROM rpg_achievements WHERE userUid = :userUid")
    suspend fun getAchievements(userUid: String): List<AchievementEntity>

    @Query("DELETE FROM rpg_achievements WHERE userUid = :userUid")
    suspend fun clearAchievements(userUid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Transaction
    suspend fun saveFullSnapshot(snapshot: RpgSaveBundle) {
        val uid = snapshot.player.userUid
        upsertPlayerState(snapshot.player)
        clearInventory(uid)
        if (snapshot.inventory.isNotEmpty()) insertInventory(snapshot.inventory)
        clearFishCollection(uid)
        if (snapshot.fishCollection.isNotEmpty()) insertFishCollection(snapshot.fishCollection)
        clearTrees(uid)
        if (snapshot.trees.isNotEmpty()) insertTrees(snapshot.trees)
        clearCrops(uid)
        if (snapshot.crops.isNotEmpty()) insertCrops(snapshot.crops)
        clearPlacedObjects(uid)
        if (snapshot.placedObjects.isNotEmpty()) insertPlacedObjects(snapshot.placedObjects)
        clearQuests(uid)
        if (snapshot.quests.isNotEmpty()) insertQuests(snapshot.quests)
        clearAchievements(uid)
        if (snapshot.achievements.isNotEmpty()) insertAchievements(snapshot.achievements)
    }
}

data class RpgSaveBundle(
    val player: PlayerStateEntity,
    val inventory: List<InventoryItemEntity>,
    val fishCollection: List<FishCollectionEntity>,
    val trees: List<TreeStateEntity>,
    val crops: List<CropPlotEntity>,
    val placedObjects: List<PlacedObjectEntity>,
    val quests: List<QuestProgressEntity>,
    val achievements: List<AchievementEntity>
)

@Database(
    entities = [
        PlayerStateEntity::class,
        InventoryItemEntity::class,
        FishCollectionEntity::class,
        TreeStateEntity::class,
        CropPlotEntity::class,
        PlacedObjectEntity::class,
        QuestProgressEntity::class,
        AchievementEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RpgDatabase : RoomDatabase() {
    abstract fun saveDao(): RpgSaveDao

    companion object {
        @Volatile
        private var INSTANCE: RpgDatabase? = null

        fun getInstance(context: Context): RpgDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RpgDatabase::class.java,
                    "willowbrook_rpg_save.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}

data class RpgLoadResult(
    val bundle: RpgSaveBundle?,
    val sourceLabel: String
)

class RpgSaveRepository(context: Context) {
    private val dao = RpgDatabase.getInstance(context).saveDao()

    suspend fun loadLocalGame(userUid: String): RpgSaveBundle? = withContext(Dispatchers.IO) {
        if (userUid.isBlank()) return@withContext null
        val player = dao.getPlayerState(userUid) ?: return@withContext null
        RpgSaveBundle(
            player = player,
            inventory = dao.getInventory(userUid),
            fishCollection = dao.getFishCollection(userUid),
            trees = dao.getTrees(userUid),
            crops = dao.getCrops(userUid),
            placedObjects = dao.getPlacedObjects(userUid),
            quests = dao.getQuests(userUid),
            achievements = dao.getAchievements(userUid)
        )
    }

    /**
     * Memuat progres terbaru untuk akun yang login:
     * Membandingkan cache lokal Room (per-UID) dengan database cloud Firestore milik user,
     * lalu otomatis menyinkronkan versi terbaru di kedua arah.
     */
    suspend fun loadLatestForUser(userUid: String): RpgLoadResult = withContext(Dispatchers.IO) {
        if (userUid.isBlank()) return@withContext RpgLoadResult(null, "Belum Login")

        val local = runCatching { loadLocalGame(userUid) }.getOrNull()
        val cloudPair = runCatching { FirebaseManager.loadRpgStateFromCloud(userUid) }.getOrNull()
        val cloudBundle = cloudPair?.first?.let { RpgSaveSerializer.fromJson(it, userUid) }
        val cloudTs = maxOf(cloudPair?.second ?: 0L, cloudBundle?.player?.updatedAtMs ?: 0L)
        val localTs = local?.player?.updatedAtMs ?: 0L

        return@withContext when {
            cloudBundle != null && (local == null || cloudTs > localTs) -> {
                runCatching { dao.saveFullSnapshot(cloudBundle) }
                RpgLoadResult(cloudBundle, "Tersinkron dari Database Akun")
            }
            local != null && (cloudBundle == null || localTs > cloudTs) -> {
                val synced = pushToCloud(local)
                RpgLoadResult(
                    local,
                    if (synced) "Tersinkron ke Database Akun" else "Tersimpan Lokal (Offline)"
                )
            }
            local != null -> {
                RpgLoadResult(local, "Tersinkron ke Database Akun")
            }
            else -> {
                RpgLoadResult(null, "Database Akun Siap")
            }
        }
    }

    suspend fun saveGame(bundle: RpgSaveBundle): Boolean = withContext(Dispatchers.IO) {
        if (bundle.player.userUid.isBlank()) return@withContext false
        runCatching { dao.saveFullSnapshot(bundle) }
        pushToCloud(bundle)
    }

    private suspend fun pushToCloud(bundle: RpgSaveBundle): Boolean {
        return runCatching {
            val uid = bundle.player.userUid
            if (uid.isBlank() || !FirebaseManager.isUserLoggedIn()) return false
            val json = RpgSaveSerializer.toJson(bundle)
            val summary = RpgSaveSerializer.buildSummaryMap(bundle)
            FirebaseManager.saveRpgStateToCloud(
                uid = uid,
                saveJson = json,
                summary = summary,
                updatedAtMs = bundle.player.updatedAtMs
            )
        }.getOrDefault(false)
    }
}

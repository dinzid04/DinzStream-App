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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.yuranime.dinz.data.firebase.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
interface RpgSaveDao {
    @Query("SELECT * FROM rpg_player_state WHERE id = 1")
    suspend fun getPlayerState(): PlayerStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerState(state: PlayerStateEntity)

    @Query("SELECT * FROM rpg_inventory")
    suspend fun getInventory(): List<InventoryItemEntity>

    @Query("DELETE FROM rpg_inventory")
    suspend fun clearInventory()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(items: List<InventoryItemEntity>)

    @Query("SELECT * FROM rpg_fish_collection")
    suspend fun getFishCollection(): List<FishCollectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFishCollection(items: List<FishCollectionEntity>)

    @Query("SELECT * FROM rpg_trees")
    suspend fun getTrees(): List<TreeStateEntity>

    @Query("DELETE FROM rpg_trees")
    suspend fun clearTrees()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrees(trees: List<TreeStateEntity>)

    @Query("SELECT * FROM rpg_crops")
    suspend fun getCrops(): List<CropPlotEntity>

    @Query("DELETE FROM rpg_crops")
    suspend fun clearCrops()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrops(crops: List<CropPlotEntity>)

    @Query("SELECT * FROM rpg_placed_objects")
    suspend fun getPlacedObjects(): List<PlacedObjectEntity>

    @Query("DELETE FROM rpg_placed_objects")
    suspend fun clearPlacedObjects()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlacedObjects(objects: List<PlacedObjectEntity>)

    @Query("SELECT * FROM rpg_quests")
    suspend fun getQuests(): List<QuestProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<QuestProgressEntity>)

    @Query("SELECT * FROM rpg_achievements")
    suspend fun getAchievements(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Transaction
    suspend fun saveFullSnapshot(snapshot: RpgSaveBundle) {
        upsertPlayerState(snapshot.player)
        clearInventory()
        if (snapshot.inventory.isNotEmpty()) insertInventory(snapshot.inventory)
        if (snapshot.fishCollection.isNotEmpty()) insertFishCollection(snapshot.fishCollection)
        clearTrees()
        if (snapshot.trees.isNotEmpty()) insertTrees(snapshot.trees)
        clearCrops()
        if (snapshot.crops.isNotEmpty()) insertCrops(snapshot.crops)
        clearPlacedObjects()
        if (snapshot.placedObjects.isNotEmpty()) insertPlacedObjects(snapshot.placedObjects)
        if (snapshot.quests.isNotEmpty()) insertQuests(snapshot.quests)
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
    version = 1,
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

class RpgSaveRepository(context: Context) {
    private val dao = RpgDatabase.getInstance(context).saveDao()

    suspend fun loadGame(): RpgSaveBundle? = withContext(Dispatchers.IO) {
        val player = dao.getPlayerState() ?: return@withContext null
        RpgSaveBundle(
            player = player,
            inventory = dao.getInventory(),
            fishCollection = dao.getFishCollection(),
            trees = dao.getTrees(),
            crops = dao.getCrops(),
            placedObjects = dao.getPlacedObjects(),
            quests = dao.getQuests(),
            achievements = dao.getAchievements()
        )
    }

    suspend fun saveGame(bundle: RpgSaveBundle) = withContext(Dispatchers.IO) {
        dao.saveFullSnapshot(bundle)
        syncSummaryToCloudIfSignedIn(bundle)
    }

    private fun syncSummaryToCloudIfSignedIn(bundle: RpgSaveBundle) {
        try {
            val uid = FirebaseManager.getCurrentUid() ?: return
            val clampedCoins = bundle.player.coins.coerceIn(0, 9_999_999)
            val clampedLevel = bundle.player.level.coerceIn(1, 500)
            val summary = mapOf(
                "level" to clampedLevel,
                "exp" to bundle.player.exp.coerceAtLeast(0),
                "coins" to clampedCoins,
                "houseTier" to bundle.player.houseTier.coerceIn(1, 3),
                "fishSpeciesCaught" to bundle.fishCollection.size,
                "dayCount" to bundle.player.dayCount,
                "updatedAt" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("rpg_saves")
                .document("starter_homestead")
                .set(summary, SetOptions.merge())
        } catch (_: Exception) {
            // Offline mode stays fully supported via Room local DB
        }
    }
}

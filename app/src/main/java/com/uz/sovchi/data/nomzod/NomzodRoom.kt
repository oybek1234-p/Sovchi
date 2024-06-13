package com.uz.sovchi.data.nomzod

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.uz.sovchi.appContext

@Database(entities = [ViewedNomzod::class,DislikedNomzod::class], version = 2)
abstract class AppRoomDatabase : RoomDatabase() {

    abstract fun viewedNomzodsDao(): ViewNomzodDao

    abstract fun dislikedNomzodsDao(): DislikedNomzodDao

    companion object {
        private var database: AppRoomDatabase? = null

        fun getInstance(): AppRoomDatabase {
            if (database != null) return database!!
            return Room.databaseBuilder(
                appContext,
                AppRoomDatabase::class.java, "myDatabase"
            ).fallbackToDestructiveMigration().build().also {
                database = it
            }
        }
    }
}


@Entity
data class ViewedNomzod(@PrimaryKey val id: String)

@Entity
data class DislikedNomzod(@PrimaryKey val id: String)


@Dao
interface ViewNomzodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setViewed(nomzod: ViewedNomzod)

    @Query("SELECT * FROM viewednomzod")
    fun getAll(): List<ViewedNomzod>

    @Delete
    fun delete(nomzod: ViewedNomzod)
}

@Dao
interface DislikedNomzodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setViewed(nomzod: DislikedNomzod)

    @Query("SELECT * FROM dislikednomzod")
    fun getAll(): List<DislikedNomzod>

    @Delete
    fun delete(nomzod: DislikedNomzod)
}
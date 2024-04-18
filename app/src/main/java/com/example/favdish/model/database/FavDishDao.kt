package com.example.favdish.model.database

import androidx.room.*
import com.example.favdish.model.entities.FavDish
import kotlinx.coroutines.flow.Flow

@Dao
interface FavDishDao {

    //Modify DB by adding data
    @Insert
    suspend fun insertFavDishDetails(favDish: FavDish)

    //Retrieve all data sorted by ID
    @Query("SELECT * FROM FAV_DISH_TABLE ORDER BY ID")
    fun getAllDishesList(): Flow<List<FavDish>>

    //Modify DB by adding data to existing data
    @Update
    suspend fun updateFavDishDetails(favDish: FavDish)

    // Retrieve all data that have favorite_dish boolean set to true
    @Query("SELECT * FROM FAV_DISH_TABLE WHERE favorite_dish = 1")
    fun getFavoriteDishesList() : Flow<List<FavDish>>

    //Modify DB by deleting data
    @Delete
    suspend fun deleteFavDishDetails(favDish: FavDish)

    // Retrieve data according to filtertype selected
    @Query("SELECT * FROM FAV_DISH_TABLE WHERE type = :filterType")
    fun getFilteredDishesList(filterType: String) : Flow<List<FavDish>>
}
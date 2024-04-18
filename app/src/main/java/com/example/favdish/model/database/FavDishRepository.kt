package com.example.favdish.model.database

import androidx.annotation.WorkerThread
import com.example.favdish.model.entities.FavDish
import kotlinx.coroutines.flow.Flow

class FavDishRepository(private val favDishDao: FavDishDao) {

    //use WorkerThread to insert data in the table
    @WorkerThread
    suspend fun insertFavDishData(favDish: FavDish){
        favDishDao.insertFavDishDetails(favDish)
    }

    //create a val of the function to extract all dishes from the table
    // it'll be used in the viewModel
    val allDishesList: Flow<List<FavDish>> = favDishDao.getAllDishesList()

    //use WorkerThread to update data in the table
    @WorkerThread
    suspend fun updateFavDishData(favDish: FavDish){
        favDishDao.updateFavDishDetails(favDish)
    }

    //create a val of the function to extract favorite from the table
    // it'll be used in the viewModel
    val favoriteDishes: Flow<List<FavDish>> = favDishDao.getFavoriteDishesList()

    //use WorkerThread to delete data from the table
    @WorkerThread
    suspend fun deleteFavDishData(favDish: FavDish){
        favDishDao.deleteFavDishDetails(favDish)
    }

    //create a function that will use the function in DAO to filter stuff
    fun filteredListDishes(value: String) : Flow<List<FavDish>> = favDishDao.getFilteredDishesList(value)

}
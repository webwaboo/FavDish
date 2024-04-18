package com.example.favdish.view.fragments

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.favdish.application.FavDishApplication
import com.example.favdish.model.entities.FavDish
import com.example.favdish.model.entities.RandomDish
import com.example.favdish.utils.Constants
import com.example.favdish.view.activities.R
import com.example.favdish.view.activities.databinding.FragmentRandomDishBinding
import com.example.favdish.viewmodel.FavDishViewModel
import com.example.favdish.viewmodel.FavDishViewModelFactory
import com.example.favdish.viewmodel.RandomDishViewModel

class RandomDishFragment : Fragment() {

    private var _binding: FragmentRandomDishBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var mRandomDishViewModel: RandomDishViewModel
    private var mProgressDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRandomDishBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    //show progress dialog
    private fun showCustomProgressDialog(){
        mProgressDialog = Dialog(requireActivity())
        //if progress dialog is not empty, show the layout
        mProgressDialog?.let {
            it.setContentView(R.layout.dialog_custom_progress)
            it.show()
        }
    }

    //hide progress dialog when we done
    private fun hideCustomProgressDialog(){
        //if progress dialog is not empty, dismiss layout
        mProgressDialog?.let {
            it.dismiss()
        }
    }

    //once view is created, set up viewModel
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //setup global var so that it creates viewModel based from RandomDishViewModel,
        //thanks to the ViewModelProvider
        mRandomDishViewModel = ViewModelProvider(this).get(RandomDishViewModel::class.java)

        //get viewModel of randomDish
        mRandomDishViewModel.getRandomRecipeFromAPI()

        //call function that get the random dish
        randomDishViewModelObserver()

        //setup listener for refreshSwipe of randomDish layout, to call new random dish when swiped
        binding.srlRandomDish.setOnRefreshListener {
            mRandomDishViewModel.getRandomRecipeFromAPI()
        }
    }

    //observer for the viewModel
    private fun randomDishViewModelObserver(){
        //check the response from randomDishResponse (if success, populate UI with recipe's data)
        mRandomDishViewModel.randomDishResponse.observe(viewLifecycleOwner
        ) { randomDishResponse ->
            randomDishResponse?.let {
                Log.i("random dish response", "$randomDishResponse.recipes[0]")

                //check if layout is still refreshing after successful response, if yes stop it
                if(binding.srlRandomDish.isRefreshing){
                    binding.srlRandomDish.isRefreshing = false
                }
                setRandomDishResponseInUI(randomDishResponse.recipes[0])
            }
        }
        //check response from randomDishLoadingError (if error)
        mRandomDishViewModel.randomDishLoadingError.observe(viewLifecycleOwner
        ) { dataError ->
            dataError?.let {
                Log.e("random dish API error", "$dataError")

                //check if layout is still refreshing after loading error, if yes stop it
                if(binding.srlRandomDish.isRefreshing){
                    binding.srlRandomDish.isRefreshing = false
                }
            }
        }
        //check response from loadRandomDish (value of boolean)
        mRandomDishViewModel.loadRandomDish.observe(viewLifecycleOwner
        ) { loadRandomDish ->
            loadRandomDish?.let {
                Log.e("random dish loading", "$loadRandomDish")

                //if it's loading and refreshing, display the progress dialog
                if (loadRandomDish && !binding.srlRandomDish.isRefreshing){
                    showCustomProgressDialog()
                //if not, hide it
                }else{
                    hideCustomProgressDialog()
                }
            }
        }
    }

    //populating UI with API data
    private fun setRandomDishResponseInUI(recipe: RandomDish.Recipe){
        //setup image
        Glide.with(requireActivity())
            .load(recipe.image)
            .centerCrop()
            .into(binding.ivDishImage)

        //setup title
        binding.tvTitle.text = recipe.title

        //setup dishtype, "other" by default
        var dishType: String = "other"
        if(recipe.dishTypes.isNotEmpty()){
            dishType = recipe.dishTypes[0]
            binding.tvType.text = dishType
        }

        //setup category
        binding.tvCategory.text = "Other"

        //setup ingredient
        var ingredients = ""
        for (value in recipe.extendedIngredients){
            //of empty, put an ingredient
            if (ingredients.isEmpty()){
                ingredients = value.original
            //if not, put a comma and a line and add next ingredient
            } else{
                ingredients = ingredients + ", \n" + value.original
            }
        }
        binding.tvIngredients.text = ingredients

        //setup cooking direction directly from html
        //if we using newer version than android N, we use this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            binding.tvCookingDirection.text = Html.fromHtml(
                recipe.instructions,
                Html.FROM_HTML_MODE_COMPACT
            )
        //else we use this old code
        } else {
            @Suppress("DEPRECATION")
            binding.tvCookingDirection.text = Html.fromHtml(recipe.instructions)
        }

        //Make sure favorite icon is unselected when new dish appear
        binding.ivFavoriteDish.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_favorite_unselected
            )
        )

        //create var to know if dish is added to favorite or not, default set to false
        var addedToFavorites = false

        //setup cooking time, convert readyInMinutes to string, and use it to replace %1$ of our string
        binding.tvCookingTime.text = resources.getString(
            R.string.lbl_estimate_cooking_time,
            recipe.readyInMinutes.toString()
        )

        //setup favorite, via clickListener
        binding.ivFavoriteDish.setOnClickListener {
            //check if dish is already added to favorite, if yes show toast
            if(addedToFavorites){
                Toast.makeText(requireActivity(), resources.getString(R.string.msg_already_added_to_favorites),
                    Toast.LENGTH_SHORT).show()
            //if not we add it to favorite
            }else{
                //create FavDish object with all the details and the favorite flag
                val randomDishDetails = FavDish(
                    recipe.image,
                    Constants.DISH_IMAGE_SOURCE_ONLINE,
                    recipe.title,
                    dishType,
                    "other",
                    ingredients,
                    recipe.readyInMinutes.toString(),
                    recipe.instructions,
                    true
                )
                //create a viewModel of favDish to call the insert method, to put our new favorite dish
                val mFavDishViewModel: FavDishViewModel by viewModels {
                    FavDishViewModelFactory((requireActivity().application as FavDishApplication).repository)
                }
                mFavDishViewModel.insert(randomDishDetails)

                //we change flag of addedToFavorite to true
                addedToFavorites = true

                //change color of icon favorite
                binding.ivFavoriteDish.setImageDrawable(
                    ContextCompat.getDrawable(requireActivity(),
                        R.drawable.ic_favorite_selected
                    ))
                Toast.makeText(requireActivity(), resources.getString(R.string.msg_added_to_favorites),
                    Toast.LENGTH_SHORT).show()
            }


        }



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
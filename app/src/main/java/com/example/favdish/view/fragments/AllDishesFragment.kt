package com.example.favdish.view.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.favdish.application.FavDishApplication
import com.example.favdish.model.entities.FavDish
import com.example.favdish.utils.Constants
import com.example.favdish.view.activities.AddUpdateDishActivity
import com.example.favdish.view.activities.MainActivity
import com.example.favdish.view.activities.R
import com.example.favdish.view.activities.databinding.DialogCustomListBinding
import com.example.favdish.view.activities.databinding.FragmentAllDishesBinding
import com.example.favdish.view.adapters.CustomListItemAdapter
import com.example.favdish.view.adapters.FavDishAdapter
import com.example.favdish.viewmodel.FavDishViewModel
import com.example.favdish.viewmodel.FavDishViewModelFactory

class AllDishesFragment : Fragment(),MenuProvider {

    private var _binding: FragmentAllDishesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val mFavDishViewModel: FavDishViewModel by viewModels{
        FavDishViewModelFactory((requireActivity().application as FavDishApplication).repository)
    }

    private lateinit var mFavDishAdapter: FavDishAdapter

    private lateinit var mCustomListDialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //val alldishesViewModel =
            //ViewModelProvider(this).get(FavDishViewModel::class.java)

        _binding = FragmentAllDishesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this,viewLifecycleOwner, Lifecycle.State.RESUMED)

        //set-up l'adapter de notre fragment
        binding.rvDishesList.layoutManager = GridLayoutManager(requireActivity(),2)
        mFavDishAdapter = FavDishAdapter(this@AllDishesFragment)
        binding.rvDishesList.adapter = mFavDishAdapter

        //observer is in place and will do stuff when notice a change
        mFavDishViewModel.allDishesList.observe(viewLifecycleOwner){
                dishes ->
            dishes.let {
                if (it.isNotEmpty()){
                    binding.rvDishesList.visibility = View.VISIBLE
                    binding.tvNoDishesAddedYet.visibility = View.GONE
                    mFavDishAdapter.dishesList(it)
                }else{
                    binding.rvDishesList.visibility = View.GONE
                    binding.tvNoDishesAddedYet.visibility = View.VISIBLE
                }
            }
        }

    }

    //navigate from AllDishesFragment to DishDetails
    fun dishDetails(favDish: FavDish){
        findNavController().navigate(AllDishesFragmentDirections.actionNavigationAllDishesToNavigationDishDetails(favDish))

        if (requireActivity() is MainActivity){
            (activity as MainActivity?)?.hideBottomNavigationView()
        }
    }

    //show a dialog to confirm suppression of dish
    fun deleteDish(dish: FavDish){
        //first create the builder
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(resources.getString(R.string.title_delete_dish))
        builder.setMessage(resources.getString(R.string.msg_delete_dish_dialog, dish.title))
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton(resources.getString(R.string.lbl_yes)){dialogInterface, _ ->
            mFavDishViewModel.delete(dish)
            dialogInterface.dismiss()
        }
        builder.setNegativeButton(resources.getString(R.string.lbl_no)){dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        //now call the alertDialog with your builder, add some settings and show
        val alertDialog: AlertDialog = builder.create()
        //Sets whether this dialog is cancelable with the BACK key
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    //create a dialog for filtering
    private fun filterDishesListDialog(){
        //create a dialog
        mCustomListDialog  = Dialog(requireActivity())
        //set binding to the dialog_custom_list.xml
        val binding: DialogCustomListBinding = DialogCustomListBinding.inflate(layoutInflater)

        //use dialog method that let you choose which view/layout to show
        mCustomListDialog.setContentView(binding.root)
        //change the title to the filter title
        binding.tvTitle.text= resources.getString(R.string.title_select_item_to_filter)

        //call the list of dish type into a val
        val dishTypes = Constants.dishTypes()
        //add a new item into list, at the top
        dishTypes.add(0, Constants.ALL_ITEMS)
        //setup recycler view layout manager
        binding.rvList.layoutManager = LinearLayoutManager(requireActivity())
        //set up adapter with activity, what to show, what is the selection
        val adapter = CustomListItemAdapter(requireActivity(), this@AllDishesFragment, dishTypes, Constants.FILTER_SELECTION)
        binding.rvList.adapter = adapter
        //show dialog
        mCustomListDialog.show()
    }

    //show bottom navigation on Resume
    override fun onResume() {
        super.onResume()
        if (requireActivity() is MainActivity){
            (activity as MainActivity?)?.showBottomNavigationView()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //inflate the menu icons
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_all_dishes,menu)
    }

    //change activity/display stuff when clicking on menu icon
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_add_dish -> {
                startActivity(Intent(requireActivity(),AddUpdateDishActivity::class.java))
                true
            }

            R.id.action_filter_dishes ->{
                filterDishesListDialog()
                return true
            }
            else -> false
        }
    }

    //filter function
    fun filterSelection(filterItemSelection: String){
        mCustomListDialog.dismiss()
        Log.i("filter selection", filterItemSelection)
        //If filter is All_ITEMS, display everything
        if(filterItemSelection == Constants.ALL_ITEMS){
            //observe the allDishesList and do stuff whether it's empty or not
            mFavDishViewModel.allDishesList.observe(viewLifecycleOwner){
                //if not empty, display recyclerview with dish list being observed
                if (it.isNotEmpty()){
                    binding.rvDishesList.visibility = View.VISIBLE
                    binding.tvNoDishesAddedYet.visibility = View.GONE
                    mFavDishAdapter.dishesList(it)
                //or else hide recycler and display message
                }else{
                    binding.rvDishesList.visibility = View.GONE
                    binding.tvNoDishesAddedYet.visibility = View.VISIBLE
                }
            }
        //or else, if filter is anything else, display the filter
        }else{
            //observe the FilteredList and do stuff whether it's empty or not
            mFavDishViewModel.getFilteredList(filterItemSelection).observe(viewLifecycleOwner){
                dishes ->
                dishes.let {
                    //if not empty, display recyclerview with dish list being observed
                    if(it.isNotEmpty()){
                        binding.rvDishesList.visibility = View.VISIBLE
                        binding.tvNoDishesAddedYet.visibility = View.GONE
                        mFavDishAdapter.dishesList(it)
                    //or else hide recycler and display message
                    }else{
                        binding.rvDishesList.visibility = View.GONE
                        binding.tvNoDishesAddedYet.visibility = View.VISIBLE
                    }

                }
            }
        }
    }

}
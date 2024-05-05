package com.uz.sovchi.ui

import android.app.AlertDialog
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.NomzodState
import com.uz.sovchi.databinding.DeleteDialogBinding
import com.uz.sovchi.databinding.NomzodSettingsBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.nomzod.NomzodViewModel
import kotlinx.coroutines.launch

class SettingsFragment : BaseFragment<NomzodSettingsBinding>() {

    override val layId: Int
        get() = R.layout.nomzod_settings

    private var nomzodId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nomzodId = arguments?.getString("nomzodId") ?: ""
    }

    private val nomzodViewModel: NomzodViewModel by activityViewModels()

    override fun viewCreated(bind: NomzodSettingsBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@SettingsFragment)

            editButton.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("nId", nomzodId)
                navigate(R.id.addNomzodFragment, bundle)
            }
            deleteButton.setOnClickListener {
                val alert = AlertDialog.Builder(requireContext())
                val binding = DeleteDialogBinding.inflate(layoutInflater, null, false)
                alert.setView(binding.root)
                val dialog = alert.create()
                binding.apply {
                    okButton.setOnClickListener {
                        nomzodViewModel.repository.deleteNomzod(nomzodId)
                        dialog.dismiss()
                        closeFragment()
                    }
                    cancelButton.setOnClickListener {
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
            topButton.setOnClickListener {
                setFragmentResultListener("payResult") { _, bundle ->
                    val name = bundle.getString("result") ?: return@setFragmentResultListener
                    val nomzod = nomzodViewModel.repository.myNomzods.find { it.id == nomzodId } ?: return@setFragmentResultListener
                    lifecycleScope.launch {
                        nomzod.tarif = name
                        nomzod.state = NomzodState.NOT_PAID
                        nomzodViewModel.repository.uploadNewMyNomzod(nomzod) {
                            val b = Bundle().apply {
                                putString("value",nomzodId)
                                putString("tarif",name)
                            }
                            navigate(R.id.paymentGetCheckFragment,b)
                        }
                    }
                }
                navigate(R.id.paymentFragment,Bundle().apply {
                    putBoolean("paid",true)
                })
            }

        }
    }
}
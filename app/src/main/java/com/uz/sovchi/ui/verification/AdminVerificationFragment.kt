package com.uz.sovchi.ui.verification

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.NomzodRepository
import com.uz.sovchi.data.nomzod.PlatformMessageType
import com.uz.sovchi.databinding.AdminVerifyFragmentBinding
import com.uz.sovchi.databinding.VerifyAlertBinding
import com.uz.sovchi.ui.base.BaseFragment
import com.uz.sovchi.ui.nomzod.NomzodViewModel
import com.uz.sovchi.ui.search.SearchViewModel

class AdminVerificationFragment : BaseFragment<AdminVerifyFragmentBinding>() {

    override val layId: Int
        get() = R.layout.admin_verify_fragment

    private val searchViewModel: SearchViewModel by viewModels()
    private val nomzodViewModel: NomzodViewModel by activityViewModels()

    enum class VerifyTypes {
        PHOTO_WRONG, INFO_WRONG, ACCEPTED, PREMIUM_ACCEPTED, DELETE
    }

    private fun showAlertFor(type: VerifyTypes, done: () -> Unit) {
        val title = when (type) {
            VerifyTypes.PHOTO_WRONG -> getString(R.string.photo_wrong)
            VerifyTypes.INFO_WRONG -> getString(R.string.info_wrong)
            VerifyTypes.ACCEPTED -> getString(R.string.accepted)
            VerifyTypes.DELETE -> getString(R.string.delete)
            VerifyTypes.PREMIUM_ACCEPTED -> getString(R.string.premium_accepted)
        }
        val binding = VerifyAlertBinding.inflate(layoutInflater, null, false)
        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedCornersDialog)
        val alert = dialog.setView(binding.root).create()
        binding.apply {
            okButton.setOnClickListener {
                alert.dismiss()
                done.invoke()
            }
            cancelButton.setOnClickListener {
                alert.dismiss()
            }
            titleView.text = title
        }
        alert.show()
    }

    private var vAdapter = VerifyNomzodAdapter({
        searchViewModel.loadNextNomzodlar()
    }, {
        navigate(R.id.addNomzodFragment, Bundle().apply {
            putString("nId", it.id)
            putBoolean("admin", true)
        })
    }, { model, type ->
        showAlertFor(type) {
            searchViewModel.nomzodlar.removeIf { it.id == model.id }
            searchViewModel.nomzodlarLive.postValue(searchViewModel.nomzodlar)
            if (type == VerifyTypes.DELETE) {
                NomzodRepository.deleteNomzod(model.id)
            }
            if (type == VerifyTypes.ACCEPTED) {
                nomzodViewModel.repository.verify(model, false)
            }
            if (type == VerifyTypes.PREMIUM_ACCEPTED) {
                nomzodViewModel.repository.verify(model, true)
            }
            if (type == VerifyTypes.INFO_WRONG) {
                nomzodViewModel.repository.rejectNomzod(
                    model.id,
                    PlatformMessageType.REJECT_WRONG_INFO
                )
            }
            if (type == VerifyTypes.PHOTO_WRONG) {
                nomzodViewModel.repository.rejectNomzod(model.id, PlatformMessageType.REJECT_PHOTO)
            }
        }
    })

    override fun viewCreated(bind: AdminVerifyFragmentBinding) {
        bind.apply {
            searchViewModel.mainFilter = SearchViewModel.FILTER_NEW
            searchViewModel.forVerify = true
            searchViewModel.nomzodlarLoading.observe(viewLifecycleOwner) {
                progressBar.isVisible = it && vAdapter.itemCount == 0
            }
            searchViewModel.nomzodlarLive.observe(viewLifecycleOwner) {
                vAdapter.submitList(it)
            }
            recyclerView.apply {
                adapter = vAdapter
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            }
            searchViewModel.loadNextNomzodlar()
        }
    }
}
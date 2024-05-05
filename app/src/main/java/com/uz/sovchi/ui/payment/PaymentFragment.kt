package com.uz.sovchi.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.NomzodTarif
import com.uz.sovchi.databinding.FragmentPaymentBinding
import com.uz.sovchi.ui.base.BaseFragment

class PaymentFragment : DialogFragment() {

    private var payAdapter = PaymentAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentPaymentBinding.inflate(inflater,container,false)
        bind.apply {
            toolbar.setUpBackButton(this@PaymentFragment)
            recyclerView.apply {
                adapter = payAdapter.apply {
                    submitList(NomzodTarif.entries)
                }
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                nextButton.setOnClickListener {
                    val nomzodTarif = payAdapter.selected
                    setFragmentResult("payResult", Bundle().apply {
                        putString("result",nomzodTarif.name)
                    })
                    findNavController().popBackStack()
                }
            }
        }
        return bind.root
    }
}
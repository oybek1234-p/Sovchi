package com.uz.sovchi.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.sovchi.data.nomzod.NomzodTarif
import com.uz.sovchi.databinding.FragmentPaymentBinding

class PaymentFragment : Fragment() {

    private var payAdapter: PaymentAdapter? = null

    private var paid = false
    private var type = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        type = arguments?.getInt("type") ?: -1
        paid = arguments?.getBoolean("paid") ?: false
        payAdapter = PaymentAdapter(type)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val bind = FragmentPaymentBinding.inflate(inflater, container, false)
        bind.apply {
            toolbar.setUpBackButton(this@PaymentFragment)
            recyclerView.apply {
                adapter = payAdapter?.apply {
                    var entries: List<NomzodTarif> = NomzodTarif.entries
                    if (paid) {
                        entries = entries.filter { it != NomzodTarif.STANDART }
                    }
                    submitList(entries)
                }
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                nextButton.setOnClickListener {
                    val nomzodTarif = payAdapter?.selected ?: return@setOnClickListener
                    setFragmentResult("payResult", Bundle().apply {
                        putString("result", nomzodTarif.name)
                    })
                    findNavController().popBackStack()
                }
            }
        }
        return bind.root
    }
}
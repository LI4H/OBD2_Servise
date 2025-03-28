package com.example.obd_servise.ui.dynamics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.obd_servise.databinding.FragmentDynamicsBinding

class DynamicsFragment : Fragment() {

    private var _binding: FragmentDynamicsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dynamicsViewModel =
            ViewModelProvider(this).get(DynamicsViewModel::class.java)

        _binding = FragmentDynamicsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
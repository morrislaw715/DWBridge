package com.nx.dwbridge.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.nx.dwbridge.databinding.FragmentHomeBinding
import com.nx.dwbridge.ui.ServerViewModel
import com.nx.dwbridge.ws.WebSocketService

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var serverViewModel: ServerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        serverViewModel = ViewModelProvider(requireActivity()).get(ServerViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMessages.adapter = MessagesAdapter()

        binding.btnToggle.setOnClickListener {
            val isRunning = serverViewModel.isRunning.value ?: false
            if (isRunning) stopServer() else startServer()
        }

        serverViewModel.isRunning.observe(viewLifecycleOwner) { running ->
            binding.btnToggle.text = if (running) "Stop" else "Start"
            binding.tvStatus.text = "Status: " + if (running) "running" else "stopped"
        }

        serverViewModel.messages.observe(viewLifecycleOwner) { list ->
            (binding.rvMessages.adapter as? MessagesAdapter)?.submitList(list)
        }

        return root
    }

    private fun startServer() {
        val portText = binding.etPort.text.toString()
        val port = portText.toIntOrNull() ?: WebSocketService.DEFAULT_PORT
        val intent = Intent(requireContext(), WebSocketService::class.java).apply {
            putExtra(WebSocketService.EXTRA_PORT, port)
        }
        ContextCompat.startForegroundService(requireContext(), intent)
        serverViewModel.setRunning(true)
    }

    private fun stopServer() {
        val intent = Intent(requireContext(), WebSocketService::class.java)
        requireContext().stopService(intent)
        serverViewModel.setRunning(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
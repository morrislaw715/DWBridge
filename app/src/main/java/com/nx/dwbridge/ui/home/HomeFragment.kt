package com.nx.dwbridge.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import com.nx.dwbridge.ws.WsMessage
import com.nx.dwbridge.scan.ScanBus

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var serverViewModel: ServerViewModel
    private var scanReceiver: BroadcastReceiver? = null

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
            // Disable port input while running
            binding.etPort.isEnabled = !running
            binding.etPort.isFocusable = !running
            binding.etPort.isFocusableInTouchMode = !running
            if (running) {
                // hide keyboard and clear focus when starting
                binding.etPort.clearFocus()
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etPort.windowToken, 0)
            }
        }

        serverViewModel.messages.observe(viewLifecycleOwner) { list ->
            (binding.rvMessages.adapter as? MessagesAdapter)?.submitList(list)
            // auto-scroll to top for newest message
            binding.rvMessages.post {
                if ((binding.rvMessages.adapter?.itemCount ?: 0) > 0) binding.rvMessages.scrollToPosition(0)
            }
        }

        // Observe ScanBus for incoming scans posted by DataWedgeReceiver
        ScanBus.scan.observe(viewLifecycleOwner) { msg ->
            msg?.let { serverViewModel.addMessage(it) }
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
        // No dynamic receiver to unregister; ScanBus uses LiveData
        _binding = null
    }
}
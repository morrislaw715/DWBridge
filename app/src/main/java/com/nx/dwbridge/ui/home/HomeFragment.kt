package com.nx.dwbridge.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nx.dwbridge.databinding.FragmentHomeBinding
import com.nx.dwbridge.ws.WebSocketService
import com.nx.dwbridge.scan.ScanBus
import com.nx.dwbridge.ws.WsMessage

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MessagesAdapter

    // Keep a local ordered list of messages for the adapter
    private val messagesList = mutableListOf<WsMessage>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MessagesAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMessages.adapter = adapter

        // Observe ScanBus and append incoming scans to the RecyclerView
        ScanBus.scan.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                // add newest at top
                messagesList.add(0, msg)
                adapter.submitList(messagesList.toList())
                binding.rvMessages.scrollToPosition(0)
            }
        }

        // Initialize switch state and UI
        var running = false
        binding.switchToggle.isChecked = running
        binding.tvStatus.text = "Status: stopped"

        binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            running = isChecked

            // Update status text
            binding.tvStatus.text = if (running) "Status: running" else "Status: stopped"

            // Disable port input while running to prevent changes
            binding.etPort.isEnabled = !running

            if (running) {
                // Start service
                val port = binding.etPort.text.toString().ifBlank { "127.0.0.1:12345" }
                val intent = Intent(requireContext(), WebSocketService::class.java)
                intent.putExtra("port", port)
                requireContext().startService(intent)
            } else {
                // Stop service
                val intent = Intent(requireContext(), WebSocketService::class.java)
                requireContext().stopService(intent)
            }
        }

        // Observe messages (simple static hookup - the adapter is updated from other parts of the app)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
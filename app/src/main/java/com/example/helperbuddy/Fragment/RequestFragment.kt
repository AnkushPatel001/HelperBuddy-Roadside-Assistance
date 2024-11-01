package com.example.helperbuddy.Fragment

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.helperbuddy.databinding.FragmentRequestBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class RequestFragment : Fragment() {

    private var _binding: FragmentRequestBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var requestsAdapter: RequestsAdapter
    private val requestsList = mutableListOf<RequestsAdapter.ServiceRequest>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var helperLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRequestBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupRecyclerView()

        fetchRequests()

        return binding.root
    }

    private fun setupRecyclerView() {
        val currentHelperId = auth.currentUser?.uid ?: ""
        val currentHelperEmail = auth.currentUser?.email ?: ""

        // Fetch helper data
        db.collection("users").document(currentHelperId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val helperName = document.getString("name") ?: "Unknown"
                    val helperPhone = document.getString("phone") ?: "Unknown"

                    // Fetch the helper's location
                    getHelperLocation { location ->
                        helperLocation = location

                        // Pass helper details and location to the adapter
                        requestsAdapter = RequestsAdapter(
                            requestsList,
                            requireContext(),
                            location,
                            helperName,
                            currentHelperEmail,
                            helperPhone
                        )

                        binding.requestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                        binding.requestsRecyclerView.adapter = requestsAdapter
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to fetch helper data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getHelperLocation(onLocationReceived: (Location?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            onLocationReceived(null)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                onLocationReceived(location)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
                onLocationReceived(null)
            }
    }

    private fun fetchRequests() {
        db.collection("requests").get()
            .addOnSuccessListener { documents ->
                requestsList.clear() // Clear existing requests to avoid duplicates
                for (document in documents) {
                    val request = document.toObject<RequestsAdapter.ServiceRequest>()
                    request.requestId = document.id // Set Firestore document ID
                    requestsList.add(request)
                }
                requestsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to fetch requests: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

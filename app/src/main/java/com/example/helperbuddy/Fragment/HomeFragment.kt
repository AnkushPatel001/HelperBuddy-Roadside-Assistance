package com.example.roadbuddy.Fragment

import Request
import RequestAdapter
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.helperbuddy.R
import com.example.helperbuddy.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.net.Uri

class HomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private lateinit var requestAdapter: RequestAdapter  // Adapter for RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Firestore and Auth
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Get the current user's ID
        val userId = auth.currentUser?.uid

        if (userId != null) {
            binding.Loading.visibility = View.VISIBLE
            // Fetch user data from Firestore
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    binding.Loading.visibility = View.GONE
                    if (document != null) {
                        // Get the user's name
                        val userName = document.getString("name")

                        // Display the name in a TextView
                        if (userName != null) {
                            binding.textView12.text = "Welcome " + userName.uppercase()
                        } else {
                            Toast.makeText(context, "Name not found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    binding.Loading.visibility = View.GONE
                    Toast.makeText(context, "Error fetching data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "User is not logged in", Toast.LENGTH_SHORT).show()
        }

        // Get the map fragment and notify when it's ready
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up RecyclerView with click listener
        setupRecyclerView()

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            getHelperLocation { helperLocation ->
                helperLocation?.let {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 16f))
                }
            }
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1000
            )
        }
    }

    // Function to fetch the helper's current location
    private fun getHelperLocation(onLocationRetrieved: (LatLng?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onLocationRetrieved(null)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val helperLatLng = LatLng(location.latitude, location.longitude)
                    onLocationRetrieved(helperLatLng)
                } else {
                    Toast.makeText(requireContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show()
                    onLocationRetrieved(null)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show()
                onLocationRetrieved(null)
            }
    }

    // Fetch the user's location from Firestore
    private fun getUserLocationFromFirestore(username: String, onLocationRetrieved: (LatLng?) -> Unit) {
        db.collection("requests").whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()
                    val lat = document.getDouble("latitude")
                    val lng = document.getDouble("longitude")
                    Log.d("HomeFragment", "Fetching location for username: $username")
                    Log.d("HomeFragment", "Latitude: $lat, Longitude: $lng")
                    if (lat != null && lng != null) {
                        onLocationRetrieved(LatLng(lat, lng))
                    } else {
                        onLocationRetrieved(null)
                    }
                } else {
                    onLocationRetrieved(null)
                }
            }
            .addOnFailureListener {
                onLocationRetrieved(null)
            }
    }

    // Show route when a RecyclerView item is clicked
    private fun showRouteForRequest(request: Request) {
        getHelperLocation { helperLocation ->
            if (helperLocation != null) {
                getUserLocationFromFirestore(request.username) { userLocation ->
                    if (userLocation != null) {
                        openGoogleMapsWithDirections(helperLocation, userLocation)
                    } else {
                        Toast.makeText(requireContext(), "User location not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Helper location not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Open Google Maps with directions from the helper's location to the user's location
    private fun openGoogleMapsWithDirections(helperLocation: LatLng, userLocation: LatLng) {
        val uri = "google.navigation:q=${userLocation.latitude},${userLocation.longitude}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        startActivity(intent)
    }

    // Set up RecyclerView with a click listener for requests
    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Pass a click listener that handles the clicked request
        requestAdapter = RequestAdapter(listOf()) { request ->
            showRouteForRequest(request)
        }

        binding.recyclerView.adapter = requestAdapter

        // Fetch and display user requests
        fetchUserRequests()
    }

    // Fetch requests from Firestore
    private fun fetchUserRequests() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("requests")
            .whereEqualTo("helperId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val requests = documents.map { doc ->
                    val service = doc.getString("service") ?: "Unknown"
                    val username = doc.getString("username") ?: "Unknown"
                    Request(service, username)  // Map to Request data model
                }
                requestAdapter.updateRequests(requests)  // Update the RecyclerView
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching requests: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

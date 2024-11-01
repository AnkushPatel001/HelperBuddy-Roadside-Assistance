package com.example.helperbuddy.Fragment

import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.helperbuddy.R
import com.example.helperbuddy.databinding.ItemRequestBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RequestsAdapter(
    private var requests: List<ServiceRequest>,
    private var context: Context,
    private var helperLocation: Location?,  // Pass the helper's location
    private var helperName: String,          // Helper's name
    private var helperEmail: String,         // Helper's email
    private var helperPhone: String           // Helper's phone
) : RecyclerView.Adapter<RequestsAdapter.RequestViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        holder.bind(request)
    }

    override fun getItemCount(): Int = requests.size

    inner class RequestViewHolder(private val binding: ItemRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(request: ServiceRequest) {
            binding.serviceType.text = request.service
            binding.username.text = request.username
            binding.statusBtn.text = request.status

            // Set status icon based on request status
            binding.statusIcon.setImageResource(
                when (request.status) {
                    "pending" -> R.drawable.pending
                    "accepted" -> R.drawable.accept
                    else -> R.drawable.inputlogin2_bg// Default icon if needed
                }
            )

            // Convert timestamp to formatted date
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val formattedDate = sdf.format(Date(request.timestamp))
            binding.timestamp.text = formattedDate

            // Show distance from helper to user
            helperLocation?.let { location ->
                val userLocation = Location("").apply {
                    latitude = request.latitude ?: 0.0
                    longitude = request.longitude ?: 0.0
                }
                val distanceInMeters = location.distanceTo(userLocation) // Distance in meters
                val distanceInKm = distanceInMeters / 1000 // Convert to kilometers
                binding.distanceTextView.text = String.format("Distance: %.2f km", distanceInKm) // Display in km
            }

            // Handle click on item if status is "pending"
            binding.root.setOnClickListener {
                if (request.status == "pending") {
                    showStatusDialog(request)
                }
            }
        }

        private fun showStatusDialog(request: ServiceRequest) {
            AlertDialog.Builder(context)
                .setTitle("Update Request")
                .setMessage("Do you want to accept or reject this request?")
                .setPositiveButton("Accept") { dialog, _ ->
                    if (request.status == "pending") {
                        acceptRequest(request)
                        dialog.dismiss()
                    } else {
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("Reject") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        private fun acceptRequest(request: ServiceRequest) {
            // Update Firestore document with new status and helper info
            db.collection("requests").document(request.requestId)
                .update(
                    mapOf(
                        "status" to "accepted",
                        "helperName" to helperName,
                        "helperEmail" to helperEmail,
                        "helperPhone" to helperPhone,
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show()
                    request.status = "accepted"
                    notifyItemChanged(adapterPosition)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to accept request: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Data class for service requests
    data class ServiceRequest(
        var requestId: String = "",  // Firestore document ID
        val service: String = "",
        val userId: String = "",
        val latitude: Double? = null,
        val longitude: Double? = null,
        val timestamp: Long = 0L,
        val username: String = "",
        val email: String = "",
        val phone: String = "",
        var status: String = "pending"  // Make mutable to update status in adapter
    )
}

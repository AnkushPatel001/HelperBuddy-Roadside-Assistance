import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.helperbuddy.R

// Use your custom Request data class
data class Request(val service: String, val username: String)

class RequestAdapter(
    private var requestList: List<Request>,  // List of requests
    private val onRequestClick: (Request) -> Unit  // Click listener passed from Fragment/Activity
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceTextView: TextView = itemView.findViewById(R.id.serviceTextView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)

        // Bind the data to the view elements
        fun bind(request: Request) {
            serviceTextView.text = request.service
            nameTextView.text = "By " + request.username

            // Set a click listener on the item
            itemView.setOnClickListener {
                showConfirmationDialog(request)  // Invoke the dialog when clicked
            }
        }

        // Function to show confirmation dialog
        private fun showConfirmationDialog(request: Request) {
            val builder = AlertDialog.Builder(itemView.context)
            builder.setTitle("Confirm Request")
            builder.setMessage("Do you want to get directions to ${request.username}?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                onRequestClick(request)  // If confirmed, invoke the callback with the clicked request
                dialog.dismiss()
            }

            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()  // Simply dismiss the dialog
            }

            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_requestaccepted, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requestList[position]
        holder.bind(request)
    }

    override fun getItemCount(): Int {
        return requestList.size
    }

    // Update the request list and refresh the RecyclerView
    fun updateRequests(newRequests: List<Request>) {
        requestList = newRequests
        notifyDataSetChanged()
    }
}

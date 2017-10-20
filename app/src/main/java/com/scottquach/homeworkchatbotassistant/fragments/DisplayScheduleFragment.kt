package com.scottquach.homeworkchatbotassistant.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.scottquach.homeworkchatbotassistant.*
import com.scottquach.homeworkchatbotassistant.R
import com.scottquach.homeworkchatbotassistant.adapters.RecyclerScheduleAdapter
import com.scottquach.homeworkchatbotassistant.models.ClassModel
import com.scottquach.homeworkchatbotassistant.models.TimeModel
import kotlinx.android.synthetic.main.fragment_display_schedule.*
import timber.log.Timber

/**
 * Handles displaying user classes to the user and editing options.
 * Activity must implement ScheduleDisplayListener to handle fragment transitions
 */
class DisplayScheduleFragment : Fragment(), RecyclerScheduleAdapter.ScheduleAdapterInterface {

    private var listener: ScheduleDisplayInterface? = null

    private lateinit var databaseReference: DatabaseReference
    private var user: FirebaseUser? = null
    private var userClasses = mutableListOf<ClassModel>()

    private var scheduleRecycler: RecyclerView? = null
    private var scheduleAdapter: RecyclerScheduleAdapter? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is ScheduleDisplayInterface) {
            listener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement ScheduleDisplayInterface")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return container?.inflate(R.layout.fragment_display_schedule)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        databaseReference = FirebaseDatabase.getInstance().reference
        user = FirebaseAuth.getInstance().currentUser

        scheduleRecycler = recycler_schedule

        userClasses = BaseApplication.getInstance().database.getClasses().toMutableList()
        setupRecyclerView()

        floating_create_class.setOnClickListener {
            listener?.switchToCreateFragment()
        }
    }

    private fun setupRecyclerView() {
        Timber.d("adapter was null")
        scheduleAdapter = RecyclerScheduleAdapter(userClasses, this@DisplayScheduleFragment)
        scheduleRecycler?.apply {
            adapter = scheduleAdapter
            layoutManager = LinearLayoutManager(context)
        }

        toggleNoClassLabel()
    }

    /**
     * Displays a label marking that the user has no classes, if they do
     * it makes it invisible
     */
    private fun toggleNoClassLabel() {
        if (text_no_classes?.visibility == View.VISIBLE && !userClasses.isEmpty()) {
            text_no_classes?.visibility = View.INVISIBLE
        }
    }

    interface ScheduleDisplayInterface {
        fun switchToCreateFragment()
    }

    override fun deleteClass(model: ClassModel, position: Int) {

        AlertDialog.Builder(context)
                .setTitle("Are you sure?")
                .setPositiveButton("Delete", object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        databaseReference.child("users").child(user!!.uid).child("classes").child(model.title).removeValue()
                        //Delete the assignments for corresponding class
                        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                dataSnapshot.child("users").child(user!!.uid).child("assignments").children
                                        .filter { it.child("userClass").value as String == model.title }
                                        .forEach { databaseReference.child("users").child(user!!.uid).child("assignments").child(it.key).removeValue() }
                                val manager = NotifyClassEndManager(context)
                                manager.startManaging()
                            }

                            override fun onCancelled(p0: DatabaseError?) {
                                Timber.e("Error loading data " + p0.toString())
                            }
                        })
                        scheduleAdapter?.removeItem(position)
                    }
                })
                .setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {

                    }
                })
                .create().show()


    }
}

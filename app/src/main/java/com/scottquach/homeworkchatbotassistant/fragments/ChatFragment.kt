package com.scottquach.homeworkchatbotassistant.fragments


import ai.api.AIConfiguration
import ai.api.RequestExtras
import ai.api.android.AIService
import ai.api.model.AIContext
import ai.api.model.AIResponse
import ai.api.model.Result
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.scottquach.homeworkchatbotassistant.*


import com.scottquach.homeworkchatbotassistant.adapters.RecyclerChatAdapter
import com.scottquach.homeworkchatbotassistant.models.MessageModel
import com.scottquach.homeworkchatbotassistant.utils.AnimationUtils
import com.scottquach.homeworkchatbotassistant.utils.NetworkUtils
import kotlinx.android.synthetic.main.fragment_chat.*
import timber.log.Timber
import java.sql.Timestamp
import java.util.ArrayList

class ChatFragment : Fragment() {

    private lateinit var aiService: AIService

    private val databaseReference = FirebaseDatabase.getInstance().reference
    private val user = FirebaseAuth.getInstance().currentUser

    private val messageHandler by lazy {
        MessageHandler(context)
    }

    private var recycler: RecyclerView? = null

    private lateinit var adapter: RecyclerChatAdapter

    private var userMessages = mutableListOf<MessageModel>()
    private lateinit var convoContext: String
    private lateinit var classContext: String

    private var listener: ChatFragment.ChatInterface? = null

    interface ChatInterface {
        fun  notifyNoInternetConnection()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return container?.inflate(R.layout.fragment_chat)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val config = ai.api.android.AIConfiguration("35b6e6bf57cf4c6dbeeb18b1753471ab",
                AIConfiguration.SupportedLanguages.English,
                ai.api.android.AIConfiguration.RecognitionEngine.System)
        aiService = AIService.getService(context, config)
//        aiService.setListener(context)

        recycler = recycler_messages

        button_send.setOnClickListener {
            if (NetworkUtils.isConnected(context)) {
                AnimationUtils.shrinkGrow(button_send,
                        resources.getInteger(android.R.integer.config_shortAnimTime))
                if (edit_input.text.isNotEmpty()) {
                    val text = edit_input.text.toString().trim()
                    addMessage(MessageType.SENT, text)
                    DoTextRequestTask().execute(text)
                    edit_input.setText("")
                }
            } else listener?.notifyNoInternetConnection()
        }
    }

    override fun onResume() {
        super.onResume()

        databaseReference.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(p0: DataSnapshot) {
                loadData(p0)
            }

            override fun onCancelled(p0: DatabaseError?) {
                Timber.d("Error loading data " + p0?.toString())
            }
        })
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is ChatFragment.ChatInterface) {
            listener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement ScheduleDisplayInterface")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun loadData(dataSnapshot: DataSnapshot) {
        userMessages.clear()
        for (ds in dataSnapshot.child("users").child(user!!.uid).child("messages").children) {
            val messageModel = MessageModel()
            messageModel.type = ds.child("type").value as Long
            messageModel.message = ds.child("message").value as String
            messageModel.timestamp = Timestamp((ds.child("timestamp").child("time").value as Long))
            userMessages.add(messageModel)
        }
        convoContext = dataSnapshot.child("users").child(user.uid).child("contexts").child("conversation").value as String
        classContext = dataSnapshot.child("users").child(user.uid).child("contexts").child("class").value as String
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = RecyclerChatAdapter(userMessages, context)
        val manager = LinearLayoutManager(context)
        manager.stackFromEnd = true
        recycler?.apply {
            adapter = this@ChatFragment.adapter
            layoutManager = manager
        }

        if (text_loading_messages?.visibility == View.VISIBLE && !userMessages.isEmpty()) {
            text_loading_messages?.visibility = View.INVISIBLE
        } else if (text_loading_messages?.visibility == View.VISIBLE) {
            text_loading_messages.text = "No Messages"
        }
    }

    private fun addMessage(messageType: Int, message: String) {
        val key = databaseReference.child("users").child(user!!.uid).child("messages").push().key
        val model = MessageModel(messageType.toLong(), message, Timestamp(System.currentTimeMillis()), key)
        databaseReference.child("users").child(user !!.uid).child("messages").child(key).setValue(model)
    }

    private fun defaultContext() {
        databaseReference.child("users").child(user!!.uid).child("contexts").child("conversation")
                .setValue(Constants.CONETEXT_DEFAULT)
    }

    private fun determineResponseActions(result: Result) {

        when (result.action) {
            Constants.ACTION_ASSIGNMENT_SPECIFIC_CLASS -> {
                Timber.d("Action was specific class")
                val params = result.parameters
                val date = params["date"]?.asString?.trim()
                val assignment = params["assignment-official"]?.asString?.trim()
                val userClass = params["class"]?.asString?.trim()

                if (date.isNullOrEmpty() || assignment.isNullOrEmpty() || userClass.isNullOrEmpty()) {
                    val textResponse = result.fulfillment.speech
                    addMessage(MessageType.RECEIVED, textResponse)
                } else {
                    messageHandler.confirmNewAssignmentSpecificClass(assignment!!, userClass!!, date!!)
                }

            }
            Constants.ACTION_ASSIGNMENT_PROMPTED_CLASS -> {
                Timber.d("Action was prompted class")
                val params = result.parameters
                val date = params["date"]?.asString?.trim()
                val assignment = params["assignment-official"]?.asString?.trim()

                if (date.isNullOrEmpty() || assignment.isNullOrEmpty()) {
                    val textResponse = result.fulfillment.speech
                    addMessage(MessageType.RECEIVED, textResponse)
                } else {
                    messageHandler.confirmNewAssignment(assignment!!, classContext, date!!)
                }
//                defaultContext()
            }
            Constants.ACTION_OVERDUE_ASSIGNMENTS -> {
                messageHandler.getOverdueAssignments(context)
            }
            Constants.ACTION_NEXT_ASSIGNMENT -> {
                messageHandler.getNextAssignment(context)
            }
            Constants.ACTION_REQUEST_HELP -> {
                messageHandler.receiveHelp()
            }
            else -> {
                val textResponse = result.fulfillment.speech
                addMessage(MessageType.RECEIVED, textResponse)
            }
        }
    }

    internal inner class DoTextRequestTask : AsyncTask<String, Void, AIResponse>() {
        private val exception: Exception? = null

        override fun doInBackground(vararg text: String): AIResponse? {
            var resp: AIResponse? = null
            try {
                resp = run {
                    val contexts = ArrayList<AIContext>()
                    contexts.add(AIContext(convoContext))
                    Timber.d("context is " + convoContext)
                    val requestExtras = RequestExtras(contexts, null)
                    aiService.textRequest(text[0], requestExtras)
                }
            } catch (e: Exception) {
                Timber.d(e)
            }
            return resp
        }

        override fun onPostExecute(response: AIResponse?) {
            if (response != null && !response.isError) {
                val result = response.result

                val params = result.parameters
                if (params != null && !params.isEmpty()) {
                    for ((key, value) in params) {
                        Timber.d(String.format("%s: %s", key, value.toString()))
                    }
                }

                Timber.d("Query:" + result.resolvedQuery +
                        "\nAction: " + result.action)
                determineResponseActions(result)
            } else {
                Timber.d("API.AI response was an error ")
            }
        }
    }
}

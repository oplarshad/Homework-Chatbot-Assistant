package com.scottquach.homeworkchatbotassistant

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.scottquach.homeworkchatbotassistant.models.AssignmentModel
import com.scottquach.homeworkchatbotassistant.models.MessageModel
import timber.log.Timber

import java.sql.Timestamp
import java.util.ArrayList

/**
 * Created by Scott Quach on 9/15/2017.
 */

class MessageHandler {

    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    private fun saveToDatabase(messageModels: List<MessageModel>) {
        for (model in messageModels) {
            databaseReference.child("users").child(user!!.uid).child("messages").child(model.key).setValue(model)
        }
    }

    private fun updateConvoContext(convoContext: String, classContext: String) {
        databaseReference.child("users").child(user!!.uid).child("contexts").child("conversation").setValue(convoContext)
        databaseReference.child("users").child(user!!.uid).child("contexts").child("class").setValue(classContext)
    }

    fun receiveWelcomeMessage(): List<MessageModel> {
        val stringMessages = arrayOf("Welcome to App Name",
                "If you haven't done so please specify your classes in the classes tab",
                "Every time you finish a class, I'll be here to ask you what homework you have whether it be a simple assignment or a big project",
                "Using advanced machine learning, you can answer naturally such as \"I have a chapter 3 summary due next class\" or \"I have to finish" + "exam in 3 days\"",
                "Or you can add assignments later by saying something such as \"I have a summary assignment for Research Writing due in 4 days",
                "Remember that the above statements are just basic examples, feel free to speak the way YOU would naturally speak")
        val messagesModels = ArrayList<MessageModel>()

        for (message in stringMessages) {
            val model = MessageModel()

            model.message = message
            model.type = MessageType.RECEIVED.toLong()
            model.key = getMessageKey()
            model.timestamp = Timestamp(System.currentTimeMillis())
            messagesModels.add(model)
        }
        Timber.d("saving welcome message")
        saveToDatabase(messagesModels)
        return messagesModels
    }

    fun promptForHomework(userClass: String): List<MessageModel> {
        val model = MessageModel()
        model.message = "Do you have any homework for $userClass?"
        model.type = MessageType.RECEIVED.toLong()
        model.key = getMessageKey()
        model.timestamp = Timestamp(System.currentTimeMillis())

        saveToDatabase(listOf(model))
        updateConvoContext(Constants.CONTEXT_PROMPT_HOMEWORK, userClass)
        return listOf(model)
    }

    fun confirmNewHomework(assignment: String, userClass: String,
                           dueDate: String) {

        val model = MessageModel()
        model.message = "Assignment \"$assignment\" for $userClass on $dueDate saved"
        model.type = MessageType.RECEIVED.toLong()
        model.key = getMessageKey()
        model.timestamp = Timestamp(System.currentTimeMillis())
        databaseReference.child("users").child(user!!.uid).child("assignments")
                .child(userClass).child(assignment).setValue(AssignmentModel(assignment, 0, dueDate))
        saveToDatabase(listOf(model))
    }

    private fun getMessageKey() = databaseReference.child("users").child(user!!.uid).child("messages").push().key

}
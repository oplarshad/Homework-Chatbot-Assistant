package com.scottquach.homeworkchatbotassistant.contracts

import android.content.Context
import com.scottquach.homeworkchatbotassistant.models.ClassModel

/**
 * Created by Scott Quach on 10/25/2017.
 */
interface DisplayScheduleContract {

    interface View {
        fun textLabelSetVisible()
        fun textLabelSetInvisible()
        fun setTextLabel(message: String)
        fun removeClass(position: Int)
        fun resetData()
        fun addData(data: List<ClassModel>)
        fun notifyNoInternet()
    }

    interface Presenter {
        fun requestLoadData()
        fun onDataLoaded(loadedData: List<ClassModel>)
        fun deleteClass(context: Context, model: ClassModel, position: Int)
    }
}
package com.suda.yzune.wakeupschedule.course_add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.suda.yzune.wakeupschedule.AppDatabase
import com.suda.yzune.wakeupschedule.bean.AppWidgetBean
import com.suda.yzune.wakeupschedule.bean.CourseBaseBean
import com.suda.yzune.wakeupschedule.bean.CourseDetailBean
import com.suda.yzune.wakeupschedule.bean.CourseEditBean
import com.suda.yzune.wakeupschedule.utils.CourseUtils
import com.suda.yzune.wakeupschedule.utils.ViewUtils

class AddCourseViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var editList: MutableList<CourseEditBean>
    lateinit var baseBean: CourseBaseBean

    private val dataBase = AppDatabase.getDatabase(application)
    private val baseDao = dataBase.courseBaseDao()
    private val detailDao = dataBase.courseDetailDao()
    private val widgetDao = dataBase.appWidgetDao()
    private val saveList = mutableListOf<CourseDetailBean>()

    var updateFlag = true
    val deleteList = arrayListOf<Int>()
    var newId = -1
    var tableId = 0
    var maxWeek = 30
    var nodes = 11

    fun judgeType(list: ArrayList<Int>): Int {
        val odd = list.filter {
            it % 2 == 1
        }
        val evenCount = maxWeek / 2
        val oddCount = maxWeek - evenCount
        // 0表示不是全部的单周也不是全部的双周
        if (oddCount == odd.size && oddCount == list.size) {
            return 1
        }
        if (evenCount == list.size && odd.isEmpty()) {
            return 2
        }
        return 0
    }

    suspend fun preSaveData() {
        saveList.clear()
        if (baseBean.id == -1) {
            updateFlag = false
            baseBean.id = newId
            editList.forEach {
                it.id = newId
            }
        } else {
            editList.forEach {
                it.id = baseBean.id
            }
        }
        if (baseBean.color == "") {
            baseBean.color = "#${Integer.toHexString(ViewUtils.getCustomizedColor(getApplication(), baseBean.id % 9))}"
        }
        for (i in editList.indices) {
            if (i !in deleteList) {
                saveList.addAll(CourseUtils.editBean2DetailBeanList(editList[i]))
            }
        }

        val selfUnique = CourseUtils.checkSelfUnique(saveList)

        if (selfUnique) {
            saveData()
        } else {
            throw Exception("自身重复")
        }
    }

    private fun saveData() {
        if (updateFlag) {
            baseDao.updateCourseBaseBean(baseBean)
            detailDao.deleteByIdOfTable(baseBean.id, baseBean.tableId)
            detailDao.insertList(saveList)
        } else {
            baseDao.insertCourseBase(baseBean)
            detailDao.insertList(saveList)
        }

    }

    suspend fun checkSameName(): CourseBaseBean? {
        return baseDao.checkSameNameInTableInThread(baseBean.courseName, baseBean.tableId)
    }

    fun initData(maxWeek: Int): MutableList<CourseEditBean> {
        editList = mutableListOf(CourseEditBean(
                tableId = tableId,
                weekList = MutableLiveData<ArrayList<Int>>().apply {
                    this.value = ArrayList<Int>().apply {
                        for (i in 1..maxWeek) {
                            this.add(i)
                        }
                    }
                }))
        return editList
    }

    suspend fun initData(id: Int, tableId: Int): List<CourseDetailBean> {
        editList = mutableListOf()
        return detailDao.getDetailByIdOfTableInThread(id, tableId)
    }

    suspend fun getLastId(): Int? {
        return baseDao.getLastIdOfTableInThread(tableId)
    }

    fun initBaseData(): CourseBaseBean {
        baseBean = CourseBaseBean(-1, "", "", tableId)
        return baseBean
    }

    suspend fun initBaseData(id: Int): CourseBaseBean {
        baseBean = CourseBaseBean(-1, "", "", tableId)
        return baseDao.getCourseByIdOfTableInThread(id, tableId)
    }

    suspend fun getScheduleWidgetIds(): List<AppWidgetBean> {
        return widgetDao.getWidgetsByBaseTypeInThread(0)
    }
}
package com.goazi.workoutmanager.view.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goazi.workoutmanager.R
import com.goazi.workoutmanager.adapter.ExerciseListAdapter
import com.goazi.workoutmanager.background.SilentForegroundService
import com.goazi.workoutmanager.databinding.CardSessionBinding
import com.goazi.workoutmanager.helper.Util
import com.goazi.workoutmanager.model.Exercise
import com.goazi.workoutmanager.model.Session
import com.goazi.workoutmanager.viewmodel.ExerciseViewModel
import com.goazi.workoutmanager.viewmodel.SessionViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ExerciseActivity : AppCompatActivity(), ExerciseListAdapter.OnExerciseCLickListener,
    View.OnClickListener, PopupMenu.OnMenuItemClickListener, Util.WorkOnClick, Util.RestOnClick,
    Util.DeleteOnClick, Util.OnTextChangedListener, TextView.OnEditorActionListener {
    companion object {
        private const val TAG = "ExerciseActivity"
    }

    //widgets
    private lateinit var rvExercise: RecyclerView
    private lateinit var llTimer: LinearLayoutCompat
    private lateinit var imgPlay: ImageView
    private lateinit var tvExerciseName: TextView
    private lateinit var tvSeconds: TextView
    private lateinit var imgStop: AppCompatImageView
    private lateinit var imgLock: AppCompatImageView
    private lateinit var imgRewind: AppCompatImageView
    private lateinit var imgPause: AppCompatImageView
    private lateinit var imgForward: AppCompatImageView
    private lateinit var fabAddExercise: FloatingActionButton
    private lateinit var clickedLLSessions: LinearLayoutCompat

    //variables
//    private lateinit var smoothScroller: SmoothScroller
//    private lateinit var exercises: List<Exercise>
//    private var exerciseCount: Int = 0
    private lateinit var viewModel: ExerciseViewModel
    private lateinit var sessionViewModel: SessionViewModel

    //    private lateinit var workoutId: String
//    private var isAddExerciseClicked: Boolean = false
//    private var isTimerRunning: Boolean = false
//    private var seconds: Long = 10
//    private var currExerciseName: String = ""
//    private var currExerciseId: String = ""
//    private var currExercisePosition: Int = 0
//    private var currSessionPosition: Int = -1
//    private lateinit var currentSession: Session
//    private var isWork: Boolean = false
//    private var isWorkoutRunning: Boolean = false
//    private var isLocked: Boolean = false
//    private lateinit var timer: CountDownTimer
//    private var dataMap: MutableMap<String?, MutableList<Session>> = HashMap()
//    private var viewMap: MutableMap<String?, MutableList<View>> = LinkedHashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_exercise)
        supportActionBar?.hide()

        viewModel = ViewModelProvider(this).get(ExerciseViewModel::class.java)
        sessionViewModel = ViewModelProvider(this).get(SessionViewModel::class.java)
        viewModel.workoutId = intent.extras!!.getString("id")
                .toString()
        viewModel.searchById(viewModel.workoutId)

        initViews()
    }

    private fun initViews() {
        fabAddExercise = findViewById(R.id.fab_add_exercise)
        fabAddExercise.setOnClickListener(this)

        imgPlay = findViewById(R.id.img_play)
        imgPlay.setOnClickListener(this)

        imgStop = findViewById(R.id.img_stop)
        imgStop.setOnClickListener(this)

        imgLock = findViewById(R.id.img_lock)
        imgLock.setOnClickListener(this)

        imgRewind = findViewById(R.id.img_rewind)
        imgRewind.setOnClickListener(this)

        imgPause = findViewById(R.id.img_pause)
        imgPause.setOnClickListener(this)

        imgForward = findViewById(R.id.img_forward)
        imgForward.setOnClickListener(this)

        tvSeconds = findViewById(R.id.tv_seconds)
        viewModel.seconds = tvSeconds.text.toString()
                .toLong() * 1000

        llTimer = findViewById(R.id.ll_timer)
        tvExerciseName = findViewById(R.id.tv_exercise_name)
        val tvWorkoutName = findViewById<TextView>(R.id.tv_workout_name)
        tvWorkoutName.text = Util.getSpacedText(intent.extras!!.getString("name")
                .toString())
        val llTitle = findViewById<ConstraintLayout>(R.id.ll_title)
        llTitle.setOnClickListener(this)

        rvExercise = findViewById(R.id.rv_exercise)
        var adapter = ExerciseListAdapter(applicationContext, viewModel.getLiveExercisesById.value, this)

        viewModel.getLiveExercisesById.observe(this, { exercises ->
            if (exercises.size > 0) {
                viewModel.currExerciseName = exercises[0].exerciseName
                viewModel.currExerciseId = exercises[0].id
            }
            viewModel.exercises = exercises
            if (viewModel.exerciseCount == 0) {
                viewModel.exerciseCount = exercises.size
                adapter = ExerciseListAdapter(applicationContext, viewModel.getLiveExercisesById.value, this)
                rvExercise.adapter = adapter
                val layoutManager = LinearLayoutManager(applicationContext)
//                layoutManager.stackFromEnd = true
                rvExercise.layoutManager = layoutManager
                rvExercise.setHasFixedSize(false)

                rvExercise.viewTreeObserver.addOnGlobalLayoutListener(object :
                    OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (!viewModel.isAddExerciseClicked) {
//                                scrollToBottom()
                            scrollToTop()
                        }
                        rvExercise.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            } else {
                adapter.updateList(exercises)
            }
        })

        /*smoothScroller = object : LinearSmoothScroller(applicationContext) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }*/
    }

    private fun setInitialValues() {
        tvExerciseName.text = getString(R.string.get_ready)
        viewModel.currExerciseName = viewModel.exercises[0].exerciseName
    }

    private fun startTimer() {
        viewModel.timer = object : CountDownTimer(viewModel.seconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "onTick: ${viewModel.seconds / 1000}")
                viewModel.seconds = millisUntilFinished
                updateCountDownText()
                if (viewModel.isWorkoutRunning) {
                    when {
                        viewModel.seconds in 3001..3999 -> viewModel.speech(getString(R.string.three))
                        viewModel.seconds in 2001..2999 -> viewModel.speech(getString(R.string.two))
                        viewModel.seconds in 1001..1999 -> viewModel.speech(getString(R.string.one))
                        viewModel.seconds < 1000L && viewModel.isWork -> viewModel.mediaBell.start()
                        viewModel.seconds < 1000L && !viewModel.isWork -> viewModel.mediaWhistle.start()
                    }
                } else {
                    when {
                        viewModel.seconds > 4000L -> viewModel.speech("Prepare")
                        viewModel.seconds in 3001..3999 -> viewModel.speech(getString(R.string.three))
                        viewModel.seconds in 2001..2999 -> viewModel.speech(getString(R.string.two))
                        viewModel.seconds in 1001..1999 -> viewModel.speech(getString(R.string.one))
                        viewModel.seconds < 1000L -> viewModel.mediaWhistle.start()
                    }
                }
            }

            override fun onFinish() {
                viewModel.isTimerRunning = false
                viewModel.isWorkoutRunning = true
                when {
                    viewModel.currSessionPosition < viewModel.dataMap[viewModel.currExerciseId]!!.size - 1 || (viewModel.currSessionPosition == viewModel.dataMap[viewModel.currExerciseId]!!.size - 1 && viewModel.isWork) -> {
                        viewModel.isWork = !viewModel.isWork
                        when {
                            viewModel.isWork -> viewModel.currSessionPosition++
                        }
                        tvExerciseName.text = viewModel.currExerciseName
                        viewModel.currentSession = viewModel.dataMap[viewModel.currExerciseId]!![viewModel.currSessionPosition]
                        viewModel.seconds = if (viewModel.isWork) viewModel.currentSession.workTime else viewModel.currentSession.restTime
                        viewModel.isTimerRunning = true
                        viewModel.timer.cancel()
                        Log.d(TAG, "Timer: cancel")
                        startTimer()
                        resetAnimation(viewModel.isWork)
                        animateView()
                        if (viewModel.isWork) {
                            viewModel.speech("${viewModel.currExerciseName}   Set${viewModel.currSessionPosition + 1}")
                        } else {
                            viewModel.speech("Rest")
                        }
                    }
                    viewModel.currExercisePosition < viewModel.exercises.size - 1 -> {
                        viewModel.currExercisePosition++
                        viewModel.currExerciseId = viewModel.exercises[viewModel.currExercisePosition].id
                        viewModel.currExerciseName = viewModel.exercises[viewModel.currExercisePosition].exerciseName
                        viewModel.currSessionPosition = 0
                        viewModel.currentSession = viewModel.dataMap[viewModel.currExerciseId]!![viewModel.currSessionPosition]
                        tvExerciseName.text = viewModel.currExerciseName
                        viewModel.isWork = !viewModel.isWork
                        viewModel.seconds = if (viewModel.isWork) viewModel.currentSession.workTime else viewModel.currentSession.restTime
                        viewModel.isTimerRunning = true
                        viewModel.timer.cancel()
                        Log.d(TAG, "Timer: cancel")
                        startTimer()
                        resetAnimation(viewModel.isWork)
                        animateView()
                        rvExercise.scrollToPosition(viewModel.currExercisePosition)
                        if (viewModel.isWork) {
                            viewModel.speech("${viewModel.currExerciseName}   Set${viewModel.currSessionPosition + 1}")
                        } else {
                            viewModel.speech("Rest")
                        }
                    }
                    else -> {
                        stopTimer()
                    }
                }
            }
        }.start()
        Log.d(TAG, "Timer start")
        viewModel.isTimerRunning = true
        imgPause.setImageResource(R.drawable.ic_pause)
    }

    private fun pauseTimer() {
        imgPause.setImageResource(R.drawable.ic_play)
        viewModel.timer.cancel()
        Log.d(TAG, "Timer: cancel")
        viewModel.isTimerRunning = false
    }

    private fun stopTimer() {
        viewModel.timer.cancel()
        Log.d(TAG, "Timer: cancel")
        viewModel.isWorkoutRunning = false
        viewModel.isTimerRunning = false
        llTimer.visibility = View.GONE
        fabAddExercise.visibility = View.VISIBLE
        imgPlay.visibility = View.VISIBLE

        viewModel.isWork = false
        viewModel.seconds = 5000
        viewModel.currExerciseName = ""
        viewModel.currExerciseId = ""
        viewModel.currExercisePosition = 0
        viewModel.currExerciseId = viewModel.exercises[0].id
        viewModel.currSessionPosition = 0
//        Util.showSnackBar(findViewById(R.id.activity_exercise), "Workout Stopped")
        resetAnimation(true)

        viewModel.currSessionPosition = -1
        val intent = Intent().setClass(applicationContext, SilentForegroundService::class.java)
        stopService(intent)
    }

    private fun animateView() {
        val sessionList: MutableList<View> = viewModel.viewMap[viewModel.currExerciseId]!!
        sessionList[viewModel.currSessionPosition].background = ContextCompat.getDrawable(applicationContext, R.drawable.session_background)
        val tv: AppCompatEditText = if (viewModel.isWork) {
            sessionList[viewModel.currSessionPosition].findViewById(R.id.tv_work_time)
        } else {
            sessionList[viewModel.currSessionPosition].findViewById(R.id.tv_rest_time)
        }
        tv.setTextColor(getColor(R.color.grey_dark))

//        tv.setBackgroundColor(getColor(R.color.teal_700))
        /*val colorFrom = ContextCompat.getColor(applicationContext, R.color.green_light)
        val colorTo = ContextCompat.getColor(applicationContext, R.color.green_dark)
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = seconds // milliseconds

        colorAnimation.addUpdateListener { animator -> tv.setBackgroundColor(animator.animatedValue as Int) }
        colorAnimation.start()*/
    }

    private fun resetAnimation(isWork: Boolean) {
        val allSessionList: MutableList<MutableList<View>> = ArrayList(viewModel.viewMap.values)
        var sessionList: MutableList<View> = mutableListOf()
        for (i in 0 until viewModel.currExercisePosition + 1) {
            sessionList = allSessionList[i] // sessions in a exercise

            for (j in 0 until sessionList.size) {
                if (viewModel.currExercisePosition == i && viewModel.currSessionPosition == j) {
                    break
                }
                sessionList[j].background = ContextCompat.getDrawable(applicationContext, R.drawable.card_background)
                val workTv: AppCompatEditText = sessionList[j].findViewById(R.id.tv_work_time)
                workTv.setTextColor(getColor(R.color.grey_dark))
                val restTv: AppCompatEditText = sessionList[j].findViewById(R.id.tv_rest_time)
                restTv.setTextColor(getColor(R.color.grey_dark))
            }
        }

        if (!isWork) {
            sessionList[viewModel.currSessionPosition].background = ContextCompat.getDrawable(applicationContext, R.drawable.session_background)
            val workTv: AppCompatEditText = sessionList[viewModel.currSessionPosition].findViewById(R.id.tv_work_time)
            workTv.setTextColor(getColor(R.color.grey_dark))
        }

        var tempSessionPosition: Int = viewModel.currSessionPosition
        if (!isWork) {
            tempSessionPosition++
        }

        for (i in viewModel.currExercisePosition until allSessionList.size) {
            sessionList = allSessionList[i] // sessions in a exercise

            for (j in tempSessionPosition until sessionList.size) {
                sessionList[tempSessionPosition].background = ContextCompat.getDrawable(applicationContext, R.drawable.card_background)
                val workTv: AppCompatEditText = sessionList[tempSessionPosition].findViewById(R.id.tv_work_time)
                workTv.setTextColor(getColor(R.color.green_dark))
                val restTv: AppCompatEditText = sessionList[tempSessionPosition].findViewById(R.id.tv_rest_time)
                restTv.setTextColor(getColor(R.color.red_dark))
                tempSessionPosition++
            }
            tempSessionPosition = 0
        }
    }

    private fun setMap(exeId: String, llSession: LinearLayoutCompat) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        executor.execute(kotlinx.coroutines.Runnable {

            val sessions: MutableList<Session> = sessionViewModel.getSessionsById(exeId)
            viewModel.dataMap[exeId] = sessions

            val childCount: Int = llSession.childCount
            val sessionList: MutableList<View> = mutableListOf()
            for (i in 0 until childCount) {
                val ll: View = llSession.getChildAt(i)
                sessionList.add(ll)
            }
            viewModel.viewMap[exeId] = sessionList
            /*try {
                for (exercise in exercises) {
                    val sessions: MutableList<Session> = sessionViewModel.getSessions(exercise.id)
                    dataMap[exercise.id] = sessions

                    val llSession: LinearLayoutCompat = sessionMap[exercise.id]!!
                    val childCount: Int = llSession.childCount
                    val sessionList: MutableList<View> = mutableListOf()
                    for (i in 0 until childCount) {
                        val ll: View = llSession.getChildAt(i)
                        sessionList.add(ll)
                    }
                    viewMap[exercise.id] = sessionList
                }
            } catch (e: Exception) {
                Log.d(TAG, "setMap: ")
            }*/
        })
    }

    private fun updateCountDownText() {
        val minutes = (viewModel.seconds / 1000).toInt() / 60
        val seconds = (viewModel.seconds / 1000).toInt() % 60
        val timeLeftFormatted: String = java.lang.String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        tvSeconds.text = timeLeftFormatted
    }

    private fun addExerciseDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val viewGroup: ViewGroup = findViewById(R.id.activity_exercise)
        val view: View = LayoutInflater.from(applicationContext)
                .inflate(R.layout.dialog_add_exercise, viewGroup, false)
        val edtExerciseName = view.findViewById<EditText>(R.id.edt_exercise_name)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btn_save)
        builder.setView(view)
        val alertDialog: AlertDialog = builder.create()
        btnSave.setOnClickListener {
            val uuid = Util.getUUID()
            sessionViewModel.insert(Session(Util.getUUID(), 10000, 5000, Util.getTimeStamp(), uuid))
            viewModel.insert(Exercise(uuid, Util.getTimeStamp(), Util.getUpperCaseInitials(edtExerciseName.text.toString()), viewModel.workoutId))
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    override fun onExerciseClick(position: Int) {
        Log.d(TAG, "onExerciseClick: ")
        editDone()
    }

    private lateinit var imgMenu: ImageView
    private lateinit var imgCheck: ImageView

    override fun onMenuClick(position: Int, imgMenu: AppCompatImageView, imgCheck: AppCompatImageView, llSessions: LinearLayoutCompat) {
        this.imgMenu = imgMenu
        this.imgCheck = imgCheck
        editDone()

        clickedLLSessions = llSessions
        viewModel.clickedMenuPosition = position

        val menu = PopupMenu(applicationContext, imgMenu)
        menu.menuInflater.inflate(R.menu.menu_edit_exercise, menu.menu)
        menu.gravity = Gravity.END
        menu.setOnMenuItemClickListener(this)
        menu.show()
    }

    override fun onCheckClick(position: Int, imgMenu: AppCompatImageView, imgCheck: AppCompatImageView, llSessions: LinearLayoutCompat) {
        imgMenu.visibility = View.VISIBLE
        imgCheck.visibility = View.GONE
        editDone()

        //hide keyboard
        this.currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }

        val childCount = llSessions.childCount

        for (i in 0 until childCount) {
            val view = llSessions.getChildAt(i)
            val edtWorkTime = view.findViewById<AppCompatEditText>(R.id.tv_work_time)
            edtWorkTime.focusable = View.NOT_FOCUSABLE
            edtWorkTime.isFocusableInTouchMode = false
            edtWorkTime.isCursorVisible = false

            val edtRestTime = view.findViewById<AppCompatEditText>(R.id.tv_rest_time)
            edtRestTime.focusable = View.NOT_FOCUSABLE
            edtRestTime.isFocusableInTouchMode = false
            edtRestTime.isCursorVisible = false
        }
    }

    private fun editSession(position: Int, llSessions: LinearLayoutCompat) {
        imgMenu.visibility = View.GONE
        imgCheck.visibility = View.VISIBLE

        val childCount = llSessions.childCount
        viewModel.isEditing = true

        val sessions: MutableList<Session> = viewModel.dataMap[viewModel.exercises[position].id]!!

        for (i in childCount - 1 downTo 0) {
            val session: Session = sessions[i]
            val view = llSessions.getChildAt(i)
            val edtWorkTime = view.findViewById<AppCompatEditText>(R.id.tv_work_time)
            edtWorkTime.focusable = View.FOCUSABLE
            edtWorkTime.isFocusableInTouchMode = true
            edtWorkTime.isCursorVisible = true
            edtWorkTime.requestFocus()
            edtWorkTime.setSelection(edtWorkTime.text.toString().length)
            edtWorkTime.setOnEditorActionListener(this)
            edtWorkTime.addTextChangedListener(Util.CustomTextChangedListener(session, true, this))

            val edtRestTime = view.findViewById<AppCompatEditText>(R.id.tv_rest_time)
            edtRestTime.focusable = View.FOCUSABLE
            edtRestTime.isFocusableInTouchMode = true
            edtRestTime.isCursorVisible = true
            edtRestTime.setOnEditorActionListener(this)
            edtRestTime.addTextChangedListener(Util.CustomTextChangedListener(session, false, this))
        }

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

        /*this.currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
            imm?.showSoftInput(view.windowToken, 0)
        }*/
    }

    override fun onTextChanged(text: String, session: Session, isWork: Boolean) {
        Log.d(TAG, "onTextChanged: text: $text || isWork: $isWork")
        if (isWork) {
            session.workTime = text.toLong()
        } else {
            session.restTime = text.toLong()
        }
        sessionViewModel.insert(session)
    }

    private fun addSessionDialog(position: Int, llSessions: LinearLayoutCompat) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val viewGroup: ViewGroup = findViewById(R.id.activity_exercise)
        val view: View = LayoutInflater.from(applicationContext)
                .inflate(R.layout.dialog_add_session, viewGroup, false)
        val edtWorkTime = view.findViewById<EditText>(R.id.edt_work_time)
        val edtRestTime = view.findViewById<EditText>(R.id.edt_rest_time)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btn_save)
        builder.setView(view)
        val alertDialog: AlertDialog = builder.create()
        btnSave.setOnClickListener {
            if (edtWorkTime.text.toString()
                        .toInt() < 3 || edtRestTime.text.toString()
                        .toInt() < 3) {
                Util.showSnackBar(findViewById(R.id.activity_exercise), "Time cannot be less than 3 seconds")
            } else {
                viewModel.isAddSessionClicked = false
                val session = Session(Util.getUUID(), edtWorkTime.text.toString()
                        .toLong() * 1000, edtRestTime.text.toString()
                        .toLong() * 1000, Util.getTimeStamp(), viewModel.exercises[position].id)
                //update UI
                val binding: CardSessionBinding = DataBindingUtil.inflate(layoutInflater, R.layout.card_session, null, false)
                binding.session = session

                val layoutParams: LinearLayoutCompat.LayoutParams = LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.MATCH_PARENT)
                layoutParams.setMargins(0, 10, 0, 0)
                binding.root.layoutParams = layoutParams
                binding.workClick = this
                binding.restClick = this
                val edtRest = binding.root.findViewById<EditText>(R.id.tv_rest_time)
                edtRest.isEnabled = true
//                edtRest.addTextChangedListener(TextViewBindingAdapter.OnTextChanged{})
                binding.deleteClick = this
                llSessions.addView(binding.root)
                //insert in db
                sessionViewModel.insert(session)

                alertDialog.dismiss()
                setMap(viewModel.exercises[position].id, llSessions)
            }
        }
        alertDialog.show()
    }

    override fun onExerciseAdded(position: Int, isLast: Boolean, llSessions: LinearLayoutCompat) {
        viewModel.isAddExerciseClicked = false

        val sessions: MutableList<Session> = sessionViewModel.getSessionsById(viewModel.exercises[position].id)

        llSessions.removeAllViews()
        for ((pos, session) in sessions.withIndex()) {
            val binding: CardSessionBinding = DataBindingUtil.inflate(layoutInflater, R.layout.card_session, null, false)
            binding.session = session

            val layoutParams: LinearLayoutCompat.LayoutParams = LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.MATCH_PARENT)
            layoutParams.setMargins(0, 10, 0, 0)
            binding.root.layoutParams = layoutParams
            val currSession = binding.root
            binding.workClick = this
            binding.restClick = this
            when {
                position < viewModel.currExercisePosition -> {
                    currSession.findViewById<AppCompatEditText>(R.id.tv_work_time)
                            .setTextColor(getColor(R.color.grey_dark))
                    currSession.findViewById<AppCompatEditText>(R.id.tv_rest_time)
                            .setTextColor(getColor(R.color.grey_dark))
                }
                position == viewModel.currExercisePosition -> {
                    when {
                        pos < viewModel.currSessionPosition -> {
                            currSession.findViewById<AppCompatEditText>(R.id.tv_work_time)
                                    .setTextColor(getColor(R.color.grey_dark))
                            currSession.findViewById<AppCompatEditText>(R.id.tv_rest_time)
                                    .setTextColor(getColor(R.color.grey_dark))
                        }
                        pos == viewModel.currSessionPosition -> {
                            when {
                                viewModel.isWork -> {
                                    currSession.findViewById<AppCompatEditText>(R.id.tv_work_time)
                                            .setTextColor(getColor(R.color.grey_dark))
                                }
                                else -> {
                                    currSession.findViewById<AppCompatEditText>(R.id.tv_work_time)
                                            .setTextColor(getColor(R.color.grey_dark))
                                    currSession.findViewById<TextView>(R.id.tv_rest_time)
                                            .setTextColor(getColor(R.color.grey_dark))
                                }
                            }
                        }
                    }
                }
            }
            llSessions.addView(binding.root)
        }
        setMap(viewModel.exercises[position].id, llSessions)
        Log.d(TAG, "onExerciseAdded: position: $position")
    }

    override fun onWorkClicked(view: View, session: Session) {
        if (viewModel.isWorkoutRunning && !viewModel.isLocked) {
            Log.d(TAG, "onWorkClicked: ")
            viewModel.isWork = true
//            val cardSession: View = view.parent.parent as View
            val llSessionTime: View = view.parent as View
            val llSessions: LinearLayoutCompat = llSessionTime.parent as LinearLayoutCompat
            val sessionIndex = llSessions.indexOfChild(llSessionTime)

            viewModel.seconds = session.workTime
            viewModel.timer.cancel()
            Log.d(TAG, "Timer: cancel")
            startTimer()

            viewModel.currentSession = session
            viewModel.currSessionPosition = sessionIndex
            viewModel.currExerciseId = session.exerciseId
            val exercise: Exercise = viewModel.getExerciseById(viewModel.currExerciseId)
            viewModel.currExerciseName = exercise.exerciseName

            for ((count, currExercise) in viewModel.exercises.withIndex()) {
                if (currExercise.id == (viewModel.currExerciseId)) {
                    viewModel.currExercisePosition = count
                    break
                }
            }

            resetAnimation(true)
            animateView()
        }
    }

    override fun onRestClicked(view: View, session: Session) {
        if (viewModel.isWorkoutRunning && !viewModel.isLocked) {
            Log.d(TAG, "onRestClicked: ")
            viewModel.isWork = false
//            val cardSession: View = view.parent.parent as View
            val llSessionTime: View = view.parent as View
            val llSessions: LinearLayoutCompat = llSessionTime.parent as LinearLayoutCompat
            val sessionIndex = llSessions.indexOfChild(llSessionTime)

            viewModel.seconds = session.restTime

            viewModel.currentSession = session
            viewModel.currSessionPosition = sessionIndex
            viewModel.currExerciseId = session.exerciseId
            val exercise: Exercise = viewModel.getExerciseById(viewModel.currExerciseId)
            viewModel.currExerciseName = exercise.exerciseName

            for ((count, currExercise) in viewModel.exercises.withIndex()) {
                if (currExercise.id == (viewModel.currExerciseId)) {
                    viewModel.currExercisePosition = count
                    break
                }
            }

            viewModel.timer.cancel()
            Log.d(TAG, "Timer: cancel")
            startTimer()
            resetAnimation(false)
            animateView()
        }
    }

    override fun onDeleteClicked(view: View, session: Session) {
        Log.d(TAG, "onDeleteClicked: ")
    }

    private fun stopWorkoutDialog(clicked: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Stop Workout?")
                .setPositiveButton("Yes") { dialog, id ->
                    Log.d(TAG, "stopWorkoutDialog: Yes")
                    viewModel.isWorkoutRunning = false
                    llTimer.visibility = View.GONE
                    stopTimer()
                    if (clicked == "back_clicked") {
                        finish()
                    } else {
                        fabAddExercise.visibility = View.VISIBLE
                        imgPlay.visibility = View.VISIBLE
                    }
                }
                .setNegativeButton("No") { dialog, id ->
                    Log.d(TAG, "stopWorkoutDialog: Cancel")
                    dialog.dismiss()
                }
        // Create the AlertDialog object and return it
        val dialog: AlertDialog = builder.create()
//        dialog.setTitle("title")
        dialog.show()
    }

    private fun showHideView() {
//        llTimer.animate().translationY(llTimer.measuredHeight.toFloat())
//        llTimer.animate().translationY(200F)
        llTimer.visibility = View.VISIBLE
//        expand(llTimer)
        fabAddExercise.visibility = View.GONE
        imgPlay.visibility = View.GONE
    }

    private fun deleteSession() {
        val session = viewModel.dataMap[viewModel.exercises[viewModel.clickedMenuPosition].id]?.get(viewModel.dataMap[viewModel.exercises[viewModel.clickedMenuPosition].id]?.size!! - 1)
        val view = clickedLLSessions[clickedLLSessions.childCount - 1]
        sessionViewModel.delete(session!!)
        runOnUiThread(kotlinx.coroutines.Runnable {
            clickedLLSessions.removeView(view)
            showSnackBar(getString(R.string.session_deleted), true, session, view, clickedLLSessions, null, null)
        })
    }

    private fun deleteExercise() {
        val exercise = viewModel.exercises[viewModel.clickedMenuPosition]
        val sessions: List<Session> = sessionViewModel.getSessionsById(exercise.id)
        viewModel.delete(exercise)
        for (set in sessions) {
            sessionViewModel.delete(set)
        }
        runOnUiThread(kotlinx.coroutines.Runnable {
            showSnackBar(getString(R.string.exercise_deleted), false, null, null, clickedLLSessions, exercise, sessions)
        })
    }

    private fun showSnackBar(msg: String, isSession: Boolean, session: Session?, view: View?, llSessions: LinearLayoutCompat, exercise: Exercise?, sessions: List<Session>?) {
        Snackbar.make(findViewById(R.id.activity_exercise), msg, Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo)) {
                    Log.d(TAG, "showSnackBar: UNDO clicked")
                    if (isSession) {
                        //undo session delete
                        sessionViewModel.insert(session!!)
                        llSessions.addView(view)
                    } else {
                        //undo exercise delete
                        if (sessions != null) {
                            for (set in sessions) {
                                sessionViewModel.insert(set)
                            }
                        }
                        viewModel.insert(exercise!!)
                    }
                }
                .show()
    }

    private fun rewind() {
        val time = if (viewModel.isWork) viewModel.currentSession.workTime else viewModel.currentSession.restTime
        if (viewModel.seconds + 10000 <= time) {
            viewModel.seconds += 10000
        } else {
            viewModel.seconds = time
        }
        viewModel.timer.cancel()
        Log.d(TAG, "Timer: cancel")
        startTimer()
    }

    private fun forward() {
        if (viewModel.seconds - 9000 >= 0) {
            viewModel.seconds -= 9000
            viewModel.timer.cancel()
            Log.d(TAG, "Timer: cancel")
            startTimer()
        } /*else {
            seconds = 0
        }*/
    }

    private fun moveUp() {
        if (viewModel.clickedMenuPosition == 0) {
            Util.showSnackBar(findViewById(R.id.activity_exercise), "Exercise Already at Top")
        } else {
            val selected = viewModel.exercises[viewModel.clickedMenuPosition].timeStamp
            val changeWith = viewModel.exercises[viewModel.clickedMenuPosition - 1].timeStamp

            var exercise = viewModel.exercises[viewModel.clickedMenuPosition]
            exercise.timeStamp = changeWith
            viewModel.insert(exercise)

            exercise = viewModel.exercises[viewModel.clickedMenuPosition - 1]
            exercise.timeStamp = selected
            viewModel.insert(exercise)
        }
    }

    private fun moveDown() {
        if (viewModel.clickedMenuPosition.equals(viewModel.exerciseCount - 1)) {
            Util.showSnackBar(findViewById(R.id.activity_exercise), "Exercise Already at Bottom")
        } else {
            val selected = viewModel.exercises[viewModel.clickedMenuPosition].timeStamp
            val changeWith = viewModel.exercises[viewModel.clickedMenuPosition + 1].timeStamp

            var exercise = viewModel.exercises[viewModel.clickedMenuPosition]
            exercise.timeStamp = changeWith
            viewModel.insert(exercise)

            exercise = viewModel.exercises[viewModel.clickedMenuPosition + 1]
            exercise.timeStamp = selected
            viewModel.insert(exercise)
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.add_session -> {
                Log.d(TAG, "onMenuItemClick: add session")
                viewModel.isAddSessionClicked = true
                addSessionDialog(viewModel.clickedMenuPosition, clickedLLSessions)
                true
            }
            R.id.edit_session -> {
                Log.d(TAG, "onMenuItemClick: add session")
                editSession(viewModel.clickedMenuPosition, clickedLLSessions)
                true
            }
            R.id.delete_set -> {
                Log.d(TAG, "onMenuItemClick: delete set")
                val executor: ExecutorService = Executors.newSingleThreadExecutor()
                executor.execute(kotlinx.coroutines.Runnable { deleteSession() })
                true
            }
            R.id.move_up -> {
                Log.d(TAG, "onMenuItemClick: move up")
                moveUp()
                true
            }
            R.id.move_down -> {
                Log.d(TAG, "onMenuItemClick: move down")
                moveDown()
                true
            }
            R.id.delete_exercise -> {
                Log.d(TAG, "onMenuItemClick: delete exercise")
                val executor: ExecutorService = Executors.newSingleThreadExecutor()
                executor.execute(kotlinx.coroutines.Runnable { deleteExercise() })
                true
            }
            else -> true
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            /*R.id.cl_add_exercise -> {
                viewModel.isAddExerciseClicked = true
                addExerciseDialog()
            }*/
            R.id.fab_add_exercise -> {
                if (viewModel.isEditing) {
                    editDone()
                    return
                }
                viewModel.isAddExerciseClicked = true
                addExerciseDialog()
            }
            R.id.img_stop -> {
                stopWorkoutDialog("stop_clicked")
            }
            R.id.img_play -> {
                if (viewModel.isEditing) {
                    editDone()
                    return
                }
                if (viewModel.exerciseCount == 0) {
                    Util.showSnackBar(findViewById(R.id.activity_exercise), "There are no exercises in this workout, add one using the button at the bottom")
                    return
                }
                Log.d(TAG, "onClick: Play")
                val intent = Intent().setClass(applicationContext, SilentForegroundService::class.java)
                startService(intent)
                showHideView()
                startTimer()
                scrollToTop()
                setInitialValues()
            }
            R.id.img_lock -> {
                Log.d(TAG, "onClick: Lock")
                viewModel.isLocked = !viewModel.isLocked
                if (viewModel.isLocked) {
                    imgLock.setImageResource(R.drawable.ic_lock)
                } else {
                    imgLock.setImageResource(R.drawable.ic_unlock)
                }
            }
            R.id.img_pause -> {
                if (!viewModel.isLocked) {
                    Log.d(TAG, "onClick: Pause")
                    if (viewModel.isTimerRunning) {
                        pauseTimer()
                    } else {
                        startTimer()
                    }
                }
            }
            R.id.img_rewind -> {
                if (!viewModel.isLocked) {
                    Log.d(TAG, "onClick: Rewind")
                    rewind()
                }
            }
            R.id.img_forward -> {
                if (!viewModel.isLocked) {
                    Log.d(TAG, "onClick: Forward")
                    forward()
                }
            }
            R.id.ll_title -> {
                editDone()
            }
        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (viewModel.isEditing) {
                editDone()
            }
        }
        return false
    }

    private fun scrollToTop() {
        viewModel.isAddExerciseClicked = false
        val layoutManager: LinearLayoutManager = rvExercise.layoutManager as LinearLayoutManager
        layoutManager.scrollToPositionWithOffset(0, 0)
    }

    private fun scrollToBottom() {
        rvExercise.scrollToPosition(viewModel.exercises.size - 1)
    }

    private fun editDone() {
        if (!this::clickedLLSessions.isInitialized && !viewModel.isEditing) {
            return
        }
        viewModel.isEditing = false
        imgMenu.visibility = View.VISIBLE
        imgCheck.visibility = View.GONE

        //hide keyboard
        this.currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }

        val childCount = clickedLLSessions.childCount

        for (i in 0 until childCount) {
            val view = clickedLLSessions.getChildAt(i)
            val edtWorkTime = view.findViewById<AppCompatEditText>(R.id.tv_work_time)
            edtWorkTime.focusable = View.NOT_FOCUSABLE
            edtWorkTime.isFocusableInTouchMode = false
            edtWorkTime.isCursorVisible = false

            val edtRestTime = view.findViewById<AppCompatEditText>(R.id.tv_rest_time)
            edtRestTime.focusable = View.NOT_FOCUSABLE
            edtRestTime.isFocusableInTouchMode = false
            edtRestTime.isCursorVisible = false
        }
    }

    override fun onBackPressed() {
        editDone()
        if (viewModel.isWorkoutRunning) stopWorkoutDialog("back_clicked") else finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        val intent = Intent().setClass(applicationContext, SilentForegroundService::class.java)
        stopService(intent)
    }

    private fun expand(v: View) {
        val matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
        val targetHeight = v.measuredHeight

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
//        v.layoutParams.height = 1
        v.visibility = View.VISIBLE
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                v.layoutParams.height = if (interpolatedTime == 1f) LinearLayoutCompat.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // Expansion speed of 1dp/ms
        a.duration = (targetHeight / v.context.resources.displayMetrics.density).toLong()
        v.startAnimation(a)
    }
}
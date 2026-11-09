package com.adrian.liftlog

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.adrian.liftlog.model.formatClock
import com.adrian.liftlog.session.ActiveWorkoutSession

class LiftLogApp : Application() {

    companion object {
        private const val BAR_TAG = "mini_workout_bar"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var timerView: TextView? = null

    private val tick = object : Runnable {
        override fun run() {
            timerView?.text = formatClock(ActiveWorkoutSession.elapsedSeconds())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = syncBar(activity)

            override fun onActivityPaused(activity: Activity) {
                handler.removeCallbacks(tick)
                timerView = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /** Shows or hides the mini bar for whichever screen just came to the front. */
    private fun syncBar(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = content.getChildAt(0)
        val existing = content.findViewWithTag<View>(BAR_TAG)
        val show = ActiveWorkoutSession.isActive && activity !is WorkoutActivity

        if (!show) {
            if (existing != null) {
                content.removeView(existing)
                setBottomMargin(root, 0)
            }
            return
        }

        val bar = existing ?: LayoutInflater.from(activity)
            .inflate(R.layout.view_mini_workout_bar, content, false)
            .also {
                content.addView(it)
                // Push the screen's own content up so the bar never covers it.
                setBottomMargin(root, (56 * activity.resources.displayMetrics.density).toInt())
            }

        bar.findViewById<TextView>(R.id.miniBarTitle).text = ActiveWorkoutSession.title
        timerView = bar.findViewById(R.id.miniBarTimer)
        timerView?.text = formatClock(ActiveWorkoutSession.elapsedSeconds())
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, 1000)

        bar.setOnClickListener {
            activity.startActivity(
                Intent(activity, WorkoutActivity::class.java).putExtra("resume_session", true)
            )
        }
    }

    private fun setBottomMargin(view: View?, px: Int) {
        val v = view ?: return
        val lp = v.layoutParams as? FrameLayout.LayoutParams ?: return
        lp.bottomMargin = px
        v.layoutParams = lp
    }
}
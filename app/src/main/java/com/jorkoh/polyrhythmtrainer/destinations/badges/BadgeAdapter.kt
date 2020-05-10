package com.jorkoh.polyrhythmtrainer.destinations.badges

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.toSimpleString
import com.jorkoh.polyrhythmtrainer.repositories.ModeIds
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.badge_row.*

class BadgeAdapter(
        private val impossibleTrophyPressed: () -> Unit,
        private val normalModesTrophyPressed: () -> Unit
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    inner class BadgeViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        private val context
            get() = containerView.context

        fun bind(badgeGroup: BadgesGroupedByPolyrhythm, impossibleTrophyPressed: () -> Unit, normalModesTrophyPressed: () -> Unit) {
            badge_row_x_number_of_beats_text.text = badgeGroup.xBeats.toString()
            badge_row_y_number_of_beats_text.text = badgeGroup.yBeats.toString()

            badge_row_impossible_trophy.alpha = if (badgeGroup.hasImpossibleMode) {
                1f
            } else {
                0.3f
            }

            badge_row_impossible_trophy.setOnClickListener {
                impossibleTrophyPressed.invoke()
            }

            val impossibleBadge = badgeGroup.badges.firstOrNull { it.modeId == ModeIds.IMPOSSIBLE.id }
            if (impossibleBadge != null) {
                badge_row_impossible_mode_icon.alpha = 1f
                badge_row_impossible_mode_measures.visibility = View.VISIBLE
                badge_row_impossible_mode_bpm.visibility = View.VISIBLE
                badge_row_impossible_mode_date.visibility = View.VISIBLE
                badge_row_impossible_mode_incomplete.visibility = View.GONE

                badge_row_impossible_mode_measures.text = context.getString(R.string.measures, impossibleBadge.completedMeasures)
                badge_row_impossible_mode_bpm.text = context.getString(R.string.bpm, impossibleBadge.bpm.toString())
                badge_row_impossible_mode_date.text = impossibleBadge.achievedAt.toSimpleString()
            } else {
                badge_row_impossible_mode_icon.alpha = 0.3f
                badge_row_impossible_mode_measures.visibility = View.GONE
                badge_row_impossible_mode_bpm.visibility = View.GONE
                badge_row_impossible_mode_date.visibility = View.GONE
                badge_row_impossible_mode_incomplete.visibility = View.VISIBLE
            }

            val hardBadge = badgeGroup.badges.firstOrNull { it.modeId == ModeIds.HARD.id }
            if (hardBadge != null) {
                badge_row_hard_mode_icon.alpha = 1f
                badge_row_hard_mode_bpm.visibility = View.VISIBLE
                badge_row_hard_mode_date.visibility = View.VISIBLE
                badge_row_hard_mode_incomplete.visibility = View.GONE

                badge_row_hard_mode_bpm.text = context.getString(R.string.bpm, hardBadge.bpm.toString())
                badge_row_hard_mode_date.text = hardBadge.achievedAt.toSimpleString()
            } else {
                badge_row_hard_mode_icon.alpha = 0.3f
                badge_row_hard_mode_bpm.visibility = View.GONE
                badge_row_hard_mode_date.visibility = View.GONE
                badge_row_hard_mode_incomplete.visibility = View.VISIBLE
            }

            val mediumBadge = badgeGroup.badges.firstOrNull { it.modeId == ModeIds.MEDIUM.id }
            if (mediumBadge != null) {
                badge_row_medium_mode_icon.alpha = 1f
                badge_row_medium_mode_bpm.visibility = View.VISIBLE
                badge_row_medium_mode_date.visibility = View.VISIBLE
                badge_row_medium_mode_incomplete.visibility = View.GONE

                badge_row_medium_mode_bpm.text = context.getString(R.string.bpm, mediumBadge.bpm.toString())
                badge_row_medium_mode_date.text = mediumBadge.achievedAt.toSimpleString()
            } else {
                badge_row_medium_mode_icon.alpha = 0.3f
                badge_row_medium_mode_bpm.visibility = View.GONE
                badge_row_medium_mode_date.visibility = View.GONE
                badge_row_medium_mode_incomplete.visibility = View.VISIBLE
            }

            val easyBadge = badgeGroup.badges.firstOrNull { it.modeId == ModeIds.EASY.id }
            if (easyBadge != null) {
                badge_row_easy_mode_icon.alpha = 1f
                badge_row_easy_mode_bpm.visibility = View.VISIBLE
                badge_row_easy_mode_date.visibility = View.VISIBLE
                badge_row_easy_mode_incomplete.visibility = View.GONE

                badge_row_easy_mode_bpm.text = context.getString(R.string.bpm, easyBadge.bpm.toString())
                badge_row_easy_mode_date.text = easyBadge.achievedAt.toSimpleString()
            } else {
                badge_row_easy_mode_icon.alpha = 0.3f
                badge_row_easy_mode_bpm.visibility = View.GONE
                badge_row_easy_mode_date.visibility = View.GONE
                badge_row_easy_mode_incomplete.visibility = View.VISIBLE
            }
        }
    }

    init {
        setHasStableIds(true)
    }

    var badgeGroups: List<BadgesGroupedByPolyrhythm> = listOf()
        set(value) {
            field = value
            // TODO notify this properly, maybe diffutils
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.badge_row, parent, false)
        return BadgeViewHolder(view)
    }

    override fun getItemCount() = badgeGroups.size

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badgeGroup = badgeGroups[position]

        holder.bind(badgeGroup, impossibleTrophyPressed, normalModesTrophyPressed)
    }

    override fun getItemId(position: Int): Long {
        val badgeGroup = badgeGroups[position]
        return (badgeGroup.xBeats * 100 + badgeGroup.yBeats).toLong()
    }
}
package ch.admin.bag.dp3t.checkin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import ch.admin.bag.dp3t.R
import ch.admin.bag.dp3t.checkin.models.CheckinInfo
import ch.admin.bag.dp3t.checkin.storage.DiaryStorage
import ch.admin.bag.dp3t.databinding.FragmentCheckOutAndEditBinding
import ch.admin.bag.dp3t.extensions.getSubtitle
import ch.admin.bag.dp3t.extensions.getSwissCovidLocationData
import ch.admin.bag.dp3t.util.StringUtil

abstract class EditCheckinBaseFragment : Fragment() {

	abstract val checkinInfo: CheckinInfo

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		return FragmentCheckOutAndEditBinding.inflate(inflater, container, false).apply {
			checkoutTitle.text = checkinInfo.venueInfo.title
			checkoutSubtitle.setText(checkinInfo.venueInfo.getSubtitle())

			toolbarCancelButton.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

			checkoutTimeArrival.setDateTime(checkinInfo.checkInTime)
			checkoutTimeArrival.setOnDateTimeChangedListener {
				checkinInfo.checkInTime = checkoutTimeArrival.getSelectedUnixTimestamp()
			}

			checkoutTimeDeparture.setDateTime(checkinInfo.checkOutTime)
			checkoutTimeDeparture.setOnDateTimeChangedListener {
				checkinInfo.checkOutTime = checkoutTimeDeparture.getSelectedUnixTimestamp()
			}
		}.root
	}

	fun performSave() {
		val context = requireContext()

		if (checkinInfo.checkInTime > checkinInfo.checkOutTime) {
			showSavingNotPossibleDialog(getString(R.string.checkout_inverse_time_alert_description), context)
			return
		}

		val hasOverlapWithOtherCheckin = checkForOverlap(checkinInfo, context)
		if (hasOverlapWithOtherCheckin) {
			showSavingNotPossibleDialog(getString(R.string.checkout_overlapping_alert_description), context)
			return
		}

		val checkinDuration = checkinInfo.checkOutTime - checkinInfo.checkInTime
		val maxCheckinTime = checkinInfo.venueInfo.getSwissCovidLocationData().automaticCheckoutDelaylMs
		if (checkinDuration > maxCheckinTime) {
			val maxDurationString = StringUtil.getShortDurationStringWithUnits(maxCheckinTime, context)
			val dialogText = context.getString(R.string.checkout_too_long_alert_text).replace("{DURATION}", maxDurationString)
			showSavingNotPossibleDialog(dialogText, context)
			return
		}

		saveEntry()
	}

	abstract fun saveEntry()

	private fun checkForOverlap(diaryEntry: CheckinInfo, context: Context): Boolean {
		val otherCheckins = DiaryStorage.getInstance(context).entries.filter { it.id != diaryEntry.id }
		return otherCheckins.any { it.checkOutTime > diaryEntry.checkInTime && diaryEntry.checkOutTime > it.checkInTime }
	}

	private fun showSavingNotPossibleDialog(message: String, context: Context) {
		AlertDialog.Builder(context, R.style.NextStep_AlertDialogStyle)
			.setTitle(R.string.checkout_overlapping_alert_title)
			.setMessage(message)
			.setNegativeButton(R.string.android_button_ok) { dialog, _ -> dialog.dismiss() }
			.show()
	}

}
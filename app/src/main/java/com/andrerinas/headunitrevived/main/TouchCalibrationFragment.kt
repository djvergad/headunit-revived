package com.andrerinas.headunitrevived.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.andrerinas.headunitrevived.App
import com.andrerinas.headunitrevived.R
import com.andrerinas.headunitrevived.utils.Settings
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/**
 * Fragment for calibrating touch offsets at screen corners.
 * Users click on colored markers at each corner to calibrate touch input offsets.
 * This is useful for head units where touch events are displaced from click locations.
 */
class TouchCalibrationFragment : Fragment() {
    private lateinit var settings: Settings
    private lateinit var calibrationView: TouchCalibrationView
    private lateinit var instructionText: TextView
    private lateinit var toolbar: MaterialToolbar
    private var saveButton: MaterialButton? = null

    private val SAVE_ITEM_ID = 1002

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_touch_calibration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settings = App.provide(requireContext()).settings

        calibrationView = view.findViewById(R.id.calibrationView)
        instructionText = view.findViewById(R.id.instructionText)
        toolbar = view.findViewById(R.id.toolbar)
        
        // Set up calibration listener
        calibrationView.setCalibrationListener(object : TouchCalibrationView.CalibrationListener {
            override fun onCalibrationChanged() {
                updateInstructions()
            }
        })

        setupToolbar()
        updateInstructions()
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        // Add the Save item with custom layout
        val saveItem = toolbar.menu.add(0, SAVE_ITEM_ID, 0, "Save")
        saveItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        saveItem.setActionView(R.layout.layout_save_button)

        // Get the button from the action view
        saveButton = saveItem.actionView?.findViewById(R.id.save_button_widget)
        saveButton?.setOnClickListener {
            saveCalibration()
        }
    }

    private fun updateInstructions() {
        val currentCorner = calibrationView.getCurrentCorner()
        val cornerNames = listOf("Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right")
        val calibratedCount = calibrationView.getCalibratedCornerCount()

        instructionText.text = if (currentCorner < 4) {
            "Click on the ${cornerNames[currentCorner]} corner marker\n(Calibrated: $calibratedCount/4)"
        } else {
            "All corners calibrated! (Calibrated: $calibratedCount/4)"
        }
        
        // Enable/disable save button based on calibration completion
        saveButton?.isEnabled = calibrationView.isCalibrationComplete()
    }

    private fun saveCalibration() {
        if (!calibrationView.isCalibrationComplete()) {
            Toast.makeText(
                requireContext(),
                "Please calibrate all 4 corners before saving",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val offsets = calibrationView.getCalibrationOffsets()
        
        android.util.Log.d("TOUCH_CAL_SAVE", "Saving calibration offsets from view: TL=${offsets[0]}, TR=${offsets[1]}, BL=${offsets[2]}, BR=${offsets[3]}")

        // Save to settings
        settings.touchOffsetTopLeftX = offsets[0].first
        settings.touchOffsetTopLeftY = offsets[0].second
        settings.touchOffsetTopRightX = offsets[1].first
        settings.touchOffsetTopRightY = offsets[1].second
        settings.touchOffsetBottomLeftX = offsets[2].first
        settings.touchOffsetBottomLeftY = offsets[2].second
        settings.touchOffsetBottomRightX = offsets[3].first
        settings.touchOffsetBottomRightY = offsets[3].second
        
        android.util.Log.d("TOUCH_CAL_SAVE", "Set values in settings object")
        
        // Force synchronous commit
        settings.commit()
        
        android.util.Log.d("TOUCH_CAL_SAVE", "Called settings.commit()")
        
        // Verify saved values immediately
        val verifyTLX = settings.touchOffsetTopLeftX
        val verifyTLY = settings.touchOffsetTopLeftY
        android.util.Log.d("TOUCH_CAL_SAVE", "Immediately after commit - TL: ($verifyTLX, $verifyTLY)")

        Toast.makeText(
            requireContext(),
            "Touch calibration saved successfully!",
            Toast.LENGTH_SHORT
        ).show()

        requireActivity().onBackPressed()
    }
}

/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanbubblessample

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.transition.Fade
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

// The AR bubble shown above a tracked barcode. Built programmatically (rather
// than inflated from XML) so the sample needs no extra resources or CardView
// dependency. Tapping the bubble flips its content between the back-end stock
// data and the raw barcode code, mirroring the native MatrixScanBubblesSample
// (gray rounded card, "Report stock count" label + shelf/back-room counts).
//
// Both content views are stacked in a FrameLayout and the hidden one is kept
// INVISIBLE (not GONE) so the card keeps a constant footprint — the content
// flips in place and the bubble never resizes or shifts off the barcode.
internal class Bubble(context: Context, data: BubbleData, code: String) {

    val root: View

    private val shelfGroup: View
    private val codeView: TextView
    private var showShelfData = true

    init {
        val density = context.resources.displayMetrics.density
        val container = FrameLayout(context).apply {
            val padH = (16 * density).toInt()
            val padV = (8 * density).toInt()
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable().apply {
                cornerRadius = 30 * density
                setColor(Color.parseColor("#D8D9DA"))
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val centered = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        )

        shelfGroup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = centered
            addView(
                TextView(context).apply {
                    text = "Report stock count"
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                },
            )
            addView(
                TextView(context).apply {
                    text = "Shelf: ${data.shelfCount} Back room: ${data.backroomCount}"
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                },
            )
        }
        codeView = TextView(context).apply {
            text = code
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = centered
            visibility = View.INVISIBLE
        }

        container.addView(shelfGroup)
        container.addView(codeView)
        container.setOnClickListener { toggleText() }

        root = container
    }

    private fun toggleText() {
        showShelfData = !showShelfData
        // Alpha-only cross-fade (no bounds change — the hidden view stays
        // INVISIBLE, so the card footprint is constant and stays anchored to the
        // barcode). 100ms matches the native sample.
        TransitionManager.beginDelayedTransition(root as ViewGroup, Fade().setDuration(100))
        shelfGroup.visibility = if (showShelfData) View.VISIBLE else View.INVISIBLE
        codeView.visibility = if (showShelfData) View.INVISIBLE else View.VISIBLE
    }
}

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Blaž Šolar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.wefika.calendar.manager;

import android.widget.LinearLayout;

import com.wefika.calendar.CollapseCalendarView;
import com.wefika.calendar.models.AbstractViewHolder;
import com.wefika.calendar.models.SizeViewHolder;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Blaz Solar on 17/04/14.
 */
public abstract class ProgressManager {

    private static final String TAG = "ProgressManager";

    @NotNull protected CollapseCalendarView mCalendarView;

    protected LinearLayout mWeeksView;
    protected AbstractViewHolder[] mViews;

    protected SizeViewHolder mCalendarHolder;
    protected SizeViewHolder mWeeksHolder;

    final int mActiveIndex;

    private boolean mInitialized = false;

    final boolean mFromMonth;

    protected ProgressManager(@NotNull CollapseCalendarView calendarView, int activeWeek, boolean fromMonth) {
        mCalendarView = calendarView;
        mActiveIndex = activeWeek;
        mFromMonth = fromMonth;
    }

    public void applyDelta(float delta) {
        float progress = getProgress(getDeltaInBounds(delta));
        apply(progress);
    }

    public void apply(float progress) {

        mCalendarHolder.animate(progress);
        mWeeksHolder.animate(progress);

        // animate views if necessary
        if (mViews != null) {
            for (AbstractViewHolder view : mViews) {
                view.animate(progress);
            }
        }

        // request layout
        mCalendarView.requestLayout();

    }

    public boolean isInitialized() {
        return mInitialized;
    }

    void setInitialized(boolean initialized) {
        mInitialized = initialized;
    }

    public int getCurrentHeight() {
        return mCalendarView.getLayoutParams().height - mCalendarHolder.getMinHeight();
    }

    public int getStartSize() {
        return 0;
    }

    public int getEndSize() {
        return mCalendarHolder.getHeight();
    }

    public abstract void finish(boolean expanded);

    public float getProgress(int distance) {
        return Math.max(0, Math.min(distance * 1f / mCalendarHolder.getHeight(), 1));
    }

    protected int getActiveIndex() {
        return mActiveIndex;
    }

    private int getDeltaInBounds(float delta) {

        if (mFromMonth) {
            return (int) Math.max(-mCalendarHolder.getHeight(), Math.min(0, delta)) + mCalendarHolder.getHeight();
        } else {
            return (int) Math.max(0, Math.min(mCalendarHolder.getHeight(), delta));
        }

    }

}

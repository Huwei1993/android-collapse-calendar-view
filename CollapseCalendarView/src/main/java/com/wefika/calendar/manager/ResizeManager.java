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

import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import com.wefika.calendar.CollapseCalendarView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Blaz Solar on 17/04/14.
 */
public class ResizeManager {

    private static final String TAG = "ResizeManager";

    /** View to resize */
    @NotNull private CollapseCalendarView mCalendarView;

    /** Distance in px until drag has started */
    private final int mTouchSlop;

    private final int mMinFlingVelocity;

    private final int mMaxFlingVelocity;

    /** Y position on {@link android.view.MotionEvent#ACTION_DOWN} */
    private float mDownY;

    /** Y position when resizing started */
    private float mDragStartY;

    /** If calendar is currently resizing. */
    private State mState = State.IDLE;

    private VelocityTracker mVelocityTracker;
    private final Scroller mScroller;

    @Nullable private ProgressManager mProgressManager;

    public ResizeManager(@NotNull CollapseCalendarView calendarView) {

        mCalendarView = calendarView;

        mScroller = new Scroller(calendarView.getContext());

        ViewConfiguration viewConfig = ViewConfiguration.get(mCalendarView.getContext());
        mTouchSlop = viewConfig.getScaledTouchSlop();
        mMinFlingVelocity = viewConfig.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = viewConfig.getScaledMaximumFlingVelocity();
    }

    public boolean onInterceptTouchEvent(@NotNull MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return onDownEvent(ev);
            case MotionEvent.ACTION_MOVE:

                mVelocityTracker.addMovement(ev);

                return checkForResizing(ev);

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                finishMotionEvent();
                return false;
        }

        return false;
    }

    public boolean onTouchEvent(@NotNull MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);

        if (action == MotionEvent.ACTION_MOVE) {
            mVelocityTracker.addMovement(event);
        }

        if (mState == State.DRAGGING) {
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    int deltaY = calculateDistanceForDrag(event);
                    mProgressManager.applyDelta(deltaY);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    finishMotionEvent();
                    break;
            }

        } else if (action == MotionEvent.ACTION_MOVE) {
            checkForResizing(event);
        }

        return true;
    }

    /**
     * Triggered
     * @param event Down event
     */
    private boolean onDownEvent(@NotNull MotionEvent event) {
        if (MotionEventCompat.getActionMasked(event) != MotionEvent.ACTION_DOWN) {
            throw new IllegalStateException("Has to be down event!");
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }

        mDownY = event.getY();

        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
            if (mScroller.getFinalY() == 0) {
                mDragStartY = mDownY + mScroller.getStartY() - mScroller.getCurrY();
            } else {
                mDragStartY = mDownY - mScroller.getCurrY();
            }
            mState = State.DRAGGING;
            return true;
        } else {
            return false;
        }

    }

    public void recycle() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    public boolean checkForResizing(MotionEvent ev) { // FIXME this method should only return true / false. Make another method for starting animation
        if (mState == State.DRAGGING) {
            return true;
        }

        final int yDIff = calculateDistance(ev);

        CalendarManager manager = mCalendarView.getManager();
        CalendarManager.State state = manager.getState();

        if (Math.abs(yDIff) > mTouchSlop) { // FIXME this should happen only if dragging int right direction
            mState = State.DRAGGING;
            mDragStartY = ev.getY();

            if (mProgressManager == null) {

                int weekOfMonth = manager.getWeekOfMonth();

                if (state == CalendarManager.State.WEEK) { // always animate in month view
                    manager.toggleView();
                    mCalendarView.populateLayout();
                }

                mProgressManager = new ProgressManagerImpl(mCalendarView, weekOfMonth,
                        state == CalendarManager.State.MONTH);
            }

            return true;
        }

        return false;
    }

    private void finishMotionEvent() {
        if (mProgressManager != null && mProgressManager.isInitialized()) {
            startScolling();
        }
    }

    private void startScolling() {

        mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
        int velocity = (int) mVelocityTracker.getYVelocity();

        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }

        int progress = mProgressManager.getCurrentHeight();
        int end;
        if (Math.abs(velocity) > mMinFlingVelocity) {

            if (velocity > 0) {
                end = mProgressManager.getEndSize() - progress;
            } else {
                end = -progress;
            }

        } else {

            int endSize = mProgressManager.getEndSize();
            if (endSize / 2 <= progress) {
                end = endSize - progress;
            } else {
                end = -progress;
            }

        }

        mScroller.startScroll(0, progress, 0, end);
        mCalendarView.postInvalidate();

        mState = State.SETTLING;

    }

    private int calculateDistance(MotionEvent event) {
        return (int) (event.getY() - mDownY);
    }

    private int calculateDistanceForDrag(MotionEvent event) {
        return (int) (event.getY() - mDragStartY);
    }

    public void onDraw() {
        if (!mScroller.isFinished()) {
            mScroller.computeScrollOffset();

            float position = mScroller.getCurrY() * 1f / mProgressManager.getEndSize();
            mProgressManager.apply(position);
            mCalendarView.postInvalidate();
        } else if (mState == State.SETTLING) {
            mState = State.IDLE;
            float position = mScroller.getCurrY() * 1f / mProgressManager.getEndSize();
            mProgressManager.finish(position > 0);
            mProgressManager = null;
        }

    }

    private enum State {
        IDLE,
        DRAGGING,
        SETTLING
    }
}

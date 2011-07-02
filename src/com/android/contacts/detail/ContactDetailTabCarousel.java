/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.detail;

import com.android.contacts.ContactLoader;
import com.android.contacts.R;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This is a horizontally scrolling carousel with 2 tabs: one to see info about the contact and
 * one to see updates from the contact.
 */
public class ContactDetailTabCarousel extends HorizontalScrollView implements OnTouchListener {

    private static final String TAG = ContactDetailTabCarousel.class.getSimpleName();

    private static final int TAB_INDEX_ABOUT = 0;
    private static final int TAB_INDEX_UPDATES = 1;
    private static final int TAB_COUNT = 2;

    private static final double TAB_WIDTH_SCREEN_PERCENTAGE = 0.75;

    private ImageView mPhotoView;
    private TextView mStatusView;

    private Listener mListener;

    private final View[] mTabs = new View[TAB_COUNT];

    private int mTabWidth;
    private int mTabHeight;
    private int mTabDisplayLabelHeight;

    private int mAllowedHorizontalScrollLength = Integer.MIN_VALUE;
    private int mAllowedVerticalScrollLength = Integer.MIN_VALUE;

    /**
     * Interface for callbacks invoked when the user interacts with the carousel.
     */
    public interface Listener {
        public void onTouchDown();
        public void onTouchUp();
        public void onScrollChanged(int l, int t, int oldl, int oldt);
        public void onTabSelected(int position);
    }

    public ContactDetailTabCarousel(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnTouchListener(this);

        Resources resources = mContext.getResources();
        mTabHeight = resources.getDimensionPixelSize(R.dimen.detail_tab_carousel_height);
        mTabDisplayLabelHeight = resources.getDimensionPixelSize(
                R.dimen.detail_tab_carousel_tab_label_height);
        mAllowedVerticalScrollLength = mTabHeight - mTabDisplayLabelHeight;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View aboutView = findViewById(R.id.tab_about);
        View updateView = findViewById(R.id.tab_update);

        TextView aboutTab = (TextView) aboutView.findViewById(R.id.label);
        aboutTab.setText(mContext.getString(R.string.contactDetailAbout));
        aboutTab.setClickable(true);
        aboutTab.setSelected(true);

        TextView updatesTab = (TextView) updateView.findViewById(R.id.label);
        updatesTab.setText(mContext.getString(R.string.contactDetailUpdates));
        updatesTab.setClickable(true);

        aboutTab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onTabSelected(TAB_INDEX_ABOUT);
            }
        });
        updatesTab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onTabSelected(TAB_INDEX_ABOUT);
            }
        });

        mTabs[TAB_INDEX_ABOUT] = aboutTab;
        mTabs[TAB_INDEX_UPDATES] = updatesTab;

        // Retrieve the photo view for the "about" tab
        mPhotoView = (ImageView) aboutView.findViewById(R.id.photo);

        // Retrieve the social update views for the "updates" tab
        mStatusView = (TextView) updateView.findViewById(R.id.status);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec);
    }

    private void measureChildren(int widthMeasureSpec) {
        int screenWidth = MeasureSpec.getSize(widthMeasureSpec);
        // Compute the width of a tab as a fraction of the screen width
        mTabWidth = (int) (TAB_WIDTH_SCREEN_PERCENTAGE * screenWidth);

        // Find the allowed scrolling length by subtracting the current visible screen width
        // from the total length of the tabs.
        mAllowedHorizontalScrollLength = mTabWidth * TAB_COUNT - screenWidth;

        // Set the child {@link LinearLayout} to be TAB_COUNT * the computed tab width so that the
        // {@link LinearLayout}'s children (which are the tabs) will evenly split that width.
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            child.measure(MeasureSpec.makeMeasureSpec(TAB_COUNT * mTabWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mTabHeight, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        mListener.onScrollChanged(l, t, oldl, oldt);
    }

    /**
     * Returns the number of pixels that this view can be scrolled horizontally.
     */
    public int getAllowedHorizontalScrollLength() {
        return mAllowedHorizontalScrollLength;
    }

    /**
     * Returns the number of pixels that this view can be scrolled vertically while still allowing
     * the tab labels to still show.
     */
    public int getAllowedVerticalScrollLength() {
        return mAllowedVerticalScrollLength;
    }

    /**
     * Updates the tab selection.
     */
    public void setCurrentTab(int position) {
        if (position < 0 || position > mTabs.length) {
            throw new IllegalStateException("Invalid position in array of tabs: " + position);
        }
        // TODO: Handle device rotation (saving and restoring state of the selected tab)
        // This will take more work because there is no tab carousel in phone landscape
        if (mTabs[position] == null) {
            return;
        }
        mTabs[position].setSelected(true);
        unselectAllOtherTabs(position);
    }

    private void unselectAllOtherTabs(int position) {
        for (int i = 0; i < mTabs.length; i++) {
            if (position != i) {
                mTabs[i].setSelected(false);
            }
        }
    }

    /**
     * Loads the data from the Loader-Result. This is the only function that has to be called
     * from the outside to fully setup the View
     */
    public void loadData(ContactLoader.Result contactData) {
        if (contactData == null) {
            return;
        }

        ContactDetailDisplayUtils.setPhoto(mContext, contactData, mPhotoView);
        ContactDetailDisplayUtils.setSocialSnippet(mContext, contactData, mStatusView);
    }

    /**
     * Set the given {@link Listener} to handle carousel events.
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mListener.onTouchDown();
                return false;
            case MotionEvent.ACTION_UP:
                mListener.onTouchUp();
                return false;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean interceptTouch = super.onInterceptTouchEvent(ev);
        if (interceptTouch) {
            mListener.onTouchDown();
        }
        return interceptTouch;
    }
}

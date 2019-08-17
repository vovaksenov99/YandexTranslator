package com.application.akscorp.yandextranslator2017;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;


import com.application.akscorp.yandextranslator2017.Utility.MyPair;
import com.application.akscorp.yandextranslator2017.Utility.MyUtility;

import androidx.appcompat.widget.AppCompatEditText;

import static java.lang.Math.max;

/**
 * Created by Aksenov Vladimir Alekseevich(Silianor) on 04.03.2017.
 * https://github.com/vovaksenov99/EditTextWithButtons
 * This is my EditText with button in EditText field. You can add indefinitely buttons count and add action to each
 */
public class EditTextWithButtons extends AppCompatEditText {

    private Drawable[] imgButtonList;
    private Runnable[] imgButtonAction;
    private int buttonHeight = 0, buttonWidth = -1;
    private boolean userWidth = false;
    private float scaleHeight = 0;
    private Context context;

    public EditTextWithButtons(Context context) {
        super(context);
        this.context = context;
        init();
    }

    EditText getEditText() {
        return EditTextWithButtons.this;
    }

    public EditTextWithButtons(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;

        init();
    }

    public EditTextWithButtons(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    /**
     * @param buttons drawable button image and action after button click
     */
    public void defineButton(MyPair<Drawable, Runnable>... buttons) {
        imgButtonList = new Drawable[buttons.length];
        imgButtonAction = new Runnable[buttons.length];
        for (int i = 0; i < buttons.length; i++) {
            imgButtonList[i] = buttons[i].first;
            imgButtonAction[i] = buttons[i].second;
        }
        init();
    }

    /**
     * @param width set buttons list width
     */
    public void setButtonWidth(int width) {
        buttonWidth = width;
        userWidth = true;
        init();
    }

    private int getButtonNumber(float x, float y) {
        EditTextWithButtons et = (EditTextWithButtons) getEditText();
        if (x > et.getWidth() - et.getPaddingRight() - buttonWidth) {
            int currentY = 0;
            int ans = -1, i = 0;
            while (i < imgButtonList.length && y > currentY) {
                currentY += imgButtonList[i].getIntrinsicHeight();
                ans++;
                i++;
            }
            return ans;
        } else
            return -1;
    }

    private void init() {
        if (imgButtonList == null)
            return;

        buttonHeight = 0;
        if (!userWidth)
            buttonWidth = -1;
        scaleHeight = 0;
        for (int i = 0; i < imgButtonList.length; i++) {
            buttonHeight += imgButtonList[i].getIntrinsicHeight();
            if (userWidth)
                scaleHeight += (float) imgButtonList[i].getIntrinsicHeight() * ((float) buttonWidth / (float) imgButtonList[i].getIntrinsicHeight());
            else
                buttonWidth = max(buttonWidth, imgButtonList[i].getIntrinsicWidth());
        }
        drawButton();
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                EditTextWithButtons et = (EditTextWithButtons) getEditText();

                if (et.getCompoundDrawables()[2] == null)
                    return false;

                if (event.getAction() != MotionEvent.ACTION_UP)
                    return false;

                getEditText().setCursorVisible(true);
                int buttonIndex = getButtonNumber(event.getX(), event.getY());
                if (buttonIndex == -1)
                    return false;
                getEditText().setCursorVisible(false);
                ((Activity) context).runOnUiThread(imgButtonAction[buttonIndex]);
                return false;
            }
        });

        /*
        this.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                //MyEditText.this.handleClearButton();

            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
        */
    }

    private Drawable mergeButtonsImage() {
        Bitmap big = Bitmap.createBitmap(buttonWidth, (int) (userWidth ? scaleHeight : buttonHeight), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(big);
        int last = 0;
        for (int i = 0; i < imgButtonList.length; i++) {
            Drawable cur = imgButtonList[i];
            cur.setBounds(0, 0, buttonWidth, cur.getIntrinsicHeight());
            Bitmap bitmap = MyUtility.drawableToBitmap(cur);
            bitmap = MyUtility.scaleDown(bitmap, buttonWidth, false);
            canvas.drawBitmap(bitmap, 0, last, null);
            last += bitmap.getHeight();

        }
        return new BitmapDrawable(getResources(), big);
    }

    private void drawButton() {
        Drawable button = mergeButtonsImage();
        button.setBounds(0, 0, buttonWidth, button.getIntrinsicHeight());
        this.setCompoundDrawables(this.getCompoundDrawables()[0], this.getCompoundDrawables()[1], button, this.getCompoundDrawables()[3]);
    }
}
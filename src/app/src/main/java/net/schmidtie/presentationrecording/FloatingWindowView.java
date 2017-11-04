package net.schmidtie.presentationrecording;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class FloatingWindowView extends RelativeLayout {
    int mScreenHeight;
    int mScreenWidth;
    int mMinHeight;
    int mMinWidth;
    int mBorder;

    public FloatingWindowView(Context context) {
        super(context);
        init();
    }

    public FloatingWindowView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public FloatingWindowView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init();
    }

    private void init() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(displayMetrics);
        this.mScreenWidth = displayMetrics.widthPixels;
        this.mScreenHeight = displayMetrics.heightPixels;
        this.mMinWidth = this.mScreenWidth / 10;
        this.mMinHeight = this.mScreenHeight / 10;
        this.mBorder = (int) (8.0f * getResources().getDisplayMetrics().density);
    }

    int xBeforeHide;
    int yBeforeHide;
    public void setHdmiDisPlay(boolean z) {
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) getLayoutParams();
        int i = layoutParams.width;
        int i2 = layoutParams.height;
        int i3 = z ? 2568 : 2584;
        if (!z) {
            this.xBeforeHide = layoutParams.x;
            this.yBeforeHide = layoutParams.y;
        }
        WindowManager.LayoutParams layoutParams2 = new WindowManager.LayoutParams(i, i2, 2005, i3, -3);
        if (z) {
            layoutParams2.x = this.xBeforeHide;
            layoutParams2.y = this.yBeforeHide;
        } else {
            layoutParams2.x = 0;
            layoutParams2.y = 0;
        }
        layoutParams2.gravity = 51;
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).updateViewLayout(this, layoutParams2);
    }
}

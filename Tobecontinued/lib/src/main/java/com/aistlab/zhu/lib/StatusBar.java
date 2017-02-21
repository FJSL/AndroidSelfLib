package com.aistlab.zhu.lib;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 *  沉浸式状态栏
 *@author : zzq<zq.zhu@aiesst.com>
 *@date   : 2017/1/7
 *@version: 1.0
 */
public class StatusBar {

    private static final String TAG = "statusBar";

    private static final String TAG_FOR_COLOR = "STATUSBAR_FOR_COLOR";
    private static final String TAG_FOR_PADDING = "STATUSBAR_FOR_PADDING";
    // 要设置状态栏的activity
    private Activity mActivity;

    private Window mWindows;
    // 状态栏高度
    private final int mStatusBarHeight;
    // 状态栏是否能够着色 默认为可着色
    private boolean tintable = true;

    private StatusBar(Activity activity) {
        this.mActivity = activity;
        this.mWindows = activity.getWindow();
        mStatusBarHeight = getStatusBarHeight(mActivity);
        // 小于android 4.4 不可操作状态栏
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            tintable = false;
            mWindows.getDecorView().setTag("cannot operation StatusBar");
        }
    }

    /**
     * 状态栏的高度
     * @param activity activity
     * @return 状态栏的高度
     */
    public static int getStatusBarHeight(Activity activity) {
        int id = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        // 通过ID 取得状态栏的高度
        return activity.getResources().getDimensionPixelOffset(id);
    }


    public static StatusBar with(Activity activity) {
        return new StatusBar(activity);
    }

    /**
     * 将状态栏设置成透明  整个屏幕会向上移动
     *  可在跟布局设个fitSystemwindow = true 来禁止屏幕上移
     * @return self
     */
    public StatusBar translucent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWindows.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 状态栏颜色可变
            mWindows.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            View decorView = mWindows.getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            mWindows.setStatusBarColor(Color.TRANSPARENT);
            // 设置根布局不为系统预留空间
            setFitSystemWindows(mActivity, false);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWindows.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 设置根布局不为系统预留空间
            setFitSystemWindows(mActivity, false);
        } else {
            Log.d(TAG, "can not set statusBar's color!");
            // 系统版本小于4.4 不可操作状态栏
            tintable = false;
        }
        return this;
    }

    /**
     * 设置跟布局是否为系统预留空间
     * @param activity activity
     * @param isFit 是否预留
     */
    private static void setFitSystemWindows(Activity activity, boolean isFit) {
        // 将第一个View 设置成不预留空间
        ViewGroup mContentView = (ViewGroup) activity.findViewById(Window.ID_ANDROID_CONTENT);
        View mChildView = mContentView.getChildAt(0);
        if (mChildView != null)
            ViewCompat.setFitsSystemWindows(mChildView, isFit);
    }

    /**
     *  用View去替换状态栏实现沉寂状态栏
     * @param ViewColor View的颜色
     * @return self
     */
    public StatusBar tintStatusBar(@ColorRes int ViewColor) {
        // 如果不可着色 则此方法不生效
        if (!tintable) return this;
        ViewGroup rootView = (ViewGroup) mWindows.getDecorView();

        // 移除padding 模式的效果
        View paddingView = rootView.findViewWithTag(TAG_FOR_PADDING);
        if (paddingView != null) {
            paddingView.setPadding(paddingView.getPaddingLeft(), paddingView.getPaddingTop() - mStatusBarHeight,
                    paddingView.getPaddingRight(), paddingView.getPaddingBottom());
        }
        try {
            // 设置fitSystemwindow = true;
            setFitSystemWindows(mActivity, true);
            View view = new View(mActivity);
            // 设置添加view模式TAG
            view.setTag(TAG_FOR_COLOR);
            view.setBackgroundColor(mActivity.getResources().getColor(ViewColor));

            LinearLayout.LayoutParams params = new LinearLayout.
                    LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mStatusBarHeight);
            // 添加layout属性
            view.setLayoutParams(params);
            // 用View 替换statusBar
            rootView.addView(view);
            return this;
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "tintStatusBar: not find to color resources");
        }
        return this;
    }

    /**
     * 状态栏着色的方案一  通过设置View的paddingTop 来占据状态栏  此时跟布局禁止适用fitSystemWindow = true;
     * 一般用在 抽屉界面为根布局的时候
     * @param view 要设置paddingTop的View
     * @param expendViewHeight 是否将padding算入到View的高度中 true  会将padding 加入到View的高中
     * @return self
     */
    public StatusBar tintStatusBar(View view, boolean expendViewHeight) {
        if (!tintable) return this; // 不生效
        // 如果以前实用过 tintStatusBar(@ColorRes int ViewColor) 来改变状态栏 则移除操作的效果
        ViewGroup rootView = (ViewGroup) mWindows.getDecorView();
        View addedView = rootView.findViewWithTag(TAG_FOR_COLOR);
        if (addedView != null) rootView.removeView(addedView);

        // 扩展View的宽高
        if (expendViewHeight) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = params.height + getStatusBarHeight(mActivity);
        }
        // 设置padding模式标志
        view.setTag(TAG_FOR_PADDING);
        // 设置内边距
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop() + mStatusBarHeight,
                view.getPaddingRight(), view.getPaddingBottom());
        return this;
    }


    /**
     *  改变状态栏的图标和文字的颜色 如果状态栏是白色 文字和icon分不清
     *
     * @param isLight 是否是白色
     * @return self
     */
    public StatusBar lightStatusBar(boolean isLight) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            int flag = mWindows.getDecorView().getSystemUiVisibility();
            if (isLight)
                flag |= (WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                        | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            else
                flag |= ~(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                        | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

            mWindows.setStatusBarColor(Color.TRANSPARENT);
            mWindows.getDecorView().setSystemUiVisibility(flag);
        }
        try {
            processFlyMe(isLight);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            processMIUI(isLight);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * 改变小米的状态栏字体颜色为黑色, 要求MIUI6以上
     */
    private void processMIUI(boolean lightStatusBar) throws Exception {
        Class<? extends Window> clazz = mWindows.getClass();
        int darkModeFlag;
        Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
        Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
        darkModeFlag = field.getInt(layoutParams);
        Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
        extraFlagField.invoke(mWindows, lightStatusBar ? darkModeFlag : 0, darkModeFlag);
    }

    /**
     * 改变魅族的状态栏字体为黑色，要求FlyMe4以上
     */
    private void processFlyMe(boolean isLightStatusBar) throws Exception {
        WindowManager.LayoutParams lp = mWindows.getAttributes();
        Class<?> instance = Class.forName("android.view.WindowManager$LayoutParams");
        int value = instance.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON").getInt(lp);
        Field field = instance.getDeclaredField("meizuFlags");
        field.setAccessible(true);
        int origin = field.getInt(lp);
        if (isLightStatusBar) {
            field.set(lp, origin | value);
        } else {
            field.set(lp, (~value) & origin);
        }
    }
}

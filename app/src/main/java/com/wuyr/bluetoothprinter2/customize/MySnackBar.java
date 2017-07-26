package com.wuyr.bluetoothprinter2.customize;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

import com.wuyr.bluetoothprinter2.Application;
import com.wuyr.bluetoothprinter2.R;

/**
 * Created by wuyr on 4/5/16 10:55 PM.
 */
public class MySnackBar {

    public static void show(@NonNull View view, @NonNull CharSequence text, int duration) {
        show(view, text, duration, null, null);
    }

    public static void show(@NonNull View view, @NonNull CharSequence text, int duration,
                            @Nullable String actionText, @Nullable View.OnClickListener listener) {
        Snackbar snackbar = Snackbar.make(view, text, duration);
        Snackbar.SnackbarLayout root = (Snackbar.SnackbarLayout) snackbar.getView();
        root.setBackgroundColor(Application.getContext().getResources().getColor(R.color.colorAccent));
        ((TextView) root.findViewById(R.id.snackbar_text)).setTextColor(
                view.getResources().getColor(R.color.menu_text_color));
        snackbar.show();
    }
}

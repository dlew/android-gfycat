package net.danlew.gfycat.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import net.danlew.gfycat.R;

public class ErrorDialog extends DialogFragment {

    public static final String TAG = ErrorDialog.class.getName();

    private IListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mListener = (IListener) activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
            .setMessage(R.string.error_loading_gif)
            .setNeutralButton(android.R.string.ok, null)
            .create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        mListener.onDismiss();
    }

    public interface IListener {

        public void onDismiss();

    }
}

package com.tanmay.biisit;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.MediaController;

public class CustomMediaController extends MediaController {

    public CustomMediaController(Context context, boolean useFastForward) {
        super(context, useFastForward);
    }

    public CustomMediaController(Context context) {
        super(context);
    }

    public void actuallyHide() {
        super.hide();
    }

    @Override
    public void hide() {
//            super.hide();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
            actuallyHide();
        return super.dispatchKeyEvent(event);
    }
}

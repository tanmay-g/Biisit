package com.tanmay.biisit;

import android.content.Context;
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
}

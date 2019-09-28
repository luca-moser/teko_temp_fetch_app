package io.lucamoser.io.tempdiff;

import android.os.Binder;

public class TempFetchBinder extends Binder {
    private TempFetcherService service;

    public TempFetchBinder(TempFetcherService service) {
        this.service = service;
    }

    TempFetcherService getService() {
        return service;
    }
}

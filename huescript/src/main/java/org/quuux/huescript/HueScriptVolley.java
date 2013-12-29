package org.quuux.huescript;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.Volley;

import java.io.File;

public class HueScriptVolley extends Volley {

    private static final int CACHE_SIZE = 50 * 1024 * 1024;
    private static RequestQueue sRequestQueue;

    public static RequestQueue getRequestQueue(final Context context) {
        if (sRequestQueue == null) {

            final HttpStack stack = new OkHttpStack(context.getApplicationContext());
            final Network network = new BasicNetwork(stack);

            sRequestQueue= new RequestQueue(new NoCache(), network);
            sRequestQueue.start();

        }

        return sRequestQueue;
    }


}

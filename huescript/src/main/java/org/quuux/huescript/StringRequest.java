package org.quuux.huescript;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;

public class StringRequest extends com.android.volley.toolbox.StringRequest {

    private static final String TAG = Log.buildTag(StringRequest.class);
    private final String mContentType;
    private final String mBody;

    public StringRequest(final int method, final String url, final Response.Listener<String> listener, final Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
        mContentType = null;
        mBody = null;
    }

    public StringRequest(final int method, final String url, final String contentType, final String body, final Response.Listener<String> listener, final Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
        mContentType = contentType;
        mBody = body;
    }

    @Override
    public String getBodyContentType() {
        Log.d(TAG, "contentType: %s", mContentType);
        return mContentType != null ? mContentType : super.getBodyContentType();
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        Log.d(TAG, "body: %s", mBody);
        return mBody != null ? mBody.getBytes() : super.getBody();
    }
}

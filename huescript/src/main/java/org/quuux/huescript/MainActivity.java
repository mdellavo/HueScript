package org.quuux.huescript;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity
        extends ActionBarActivity
        implements AdapterView.OnItemClickListener {

    private static final String TAG = Log.buildTag(MainActivity.class);

    private GridView mListView;
    private Adapter mAdapter;
    private boolean mBound;
    private SandboxService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mAdapter = new Adapter(this);

        mListView = (GridView) findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, SandboxService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReciever);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SandboxService.ACTION_SCRIPTS_UPDATED);
        filter.addAction(Sandbox.ACTION_CALL_START);
        filter.addAction(Sandbox.ACTION_CALL_END);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReciever, filter);
        if (mBound)
            loadScripts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        Sandbox script = mAdapter.getItem(position);
        Log.d(TAG, "run script %s", script.getName());
        mService.runSandbox(script);
    }

    private void loadScripts() {
        mAdapter.clear();
        for (Sandbox sandbox : mService.getScripts()) {
            mAdapter.add(sandbox);
        }
        mAdapter.notifyDataSetChanged();
    }
    static class Holder {
        TextView scriptName;
        TextView scriptDescription;
    }

    class Adapter extends ArrayAdapter<Sandbox> {

        public Adapter(final Context context) {
            super(context, 0);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final View v = convertView != null ? convertView : newView(parent);
            bindView(v, getItem(position));
            return v;
        }

        private View newView(final ViewGroup parent) {
            final LayoutInflater inflater = getLayoutInflater();
            final View v = inflater.inflate(R.layout.list_item, parent, false);

            final Holder holder = new Holder();
            holder.scriptName = (TextView)v.findViewById(R.id.script_name);
            holder.scriptDescription = (TextView)v.findViewById(R.id.script_description);

            v.setTag(holder);

            return v;
        }

        private void bindView(final View v, final Sandbox item) {
            final Holder holder = (Holder) v.getTag();
            holder.scriptName.setText(item.getScriptName());
            holder.scriptDescription.setText(item.getScriptDescription());
            holder.scriptName.setTypeface(null, mService.isRunning(item) ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SandboxService.LocalBinder binder = (SandboxService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            loadScripts();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private BroadcastReceiver mReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d(TAG, "broadcast: %s", intent);

            final String action = intent.getAction();
            if (SandboxService.ACTION_SCRIPTS_UPDATED.equals(action))
                loadScripts();
            else if (Sandbox.ACTION_CALL_START.equals(action) || Sandbox.ACTION_CALL_END.equals(action))
                mAdapter.notifyDataSetChanged();
        }
    };

}

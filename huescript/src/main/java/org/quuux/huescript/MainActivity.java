package org.quuux.huescript;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
import android.os.FileObserver;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity
        extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<List<Sandbox>>, AdapterView.OnItemClickListener {

    private static final String TAG = Log.buildTag(MainActivity.class);
    private ListView mListView;
    private Adapter mAdapter;
    private FileObserver mObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mAdapter = new Adapter(this);

        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        getSupportLoaderManager().initLoader(0, null, this).forceLoad();

        final int mask = FileObserver.CREATE
                | FileObserver.DELETE
                | FileObserver.MODIFY
                | FileObserver.MOVED_FROM
                | FileObserver.MOVED_TO;

        mObserver = new FileObserver(getScriptsDir().getAbsolutePath(), mask) {
            @Override
            public void onEvent(final int event, final String path) {
                if (path != null) {
                    Log.d(TAG, "file changed: %s", path);
                    getSupportLoaderManager().restartLoader(0, null, MainActivity.this).forceLoad();
                }
            }
        };

        mObserver.startWatching();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mObserver.stopWatching();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public android.support.v4.content.Loader<List<Sandbox>> onCreateLoader(final int i, final Bundle bundle) {
        return new ScriptLoader(this, getScriptsDir());
    }

    @Override
    public void onLoadFinished(final android.support.v4.content.Loader<List<Sandbox>> listLoader, final List<Sandbox> sandboxes) {
        for (final Sandbox s : sandboxes)
            mAdapter.add(s);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(final android.support.v4.content.Loader<List<Sandbox>> listLoader) {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        final Sandbox sandbox = mAdapter.getItem(position);
        new Thread(new Runnable() {
            @Override
            public void run() {
                sandbox.run(MainActivity.this);
            }
        }).start();
    }

    private File getScriptsDir() {
        return new File(Environment.getExternalStorageDirectory(), "HueScripts");
    }

    static class Holder {
        TextView name;
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
            holder.name = (TextView)v.findViewById(R.id.name);
            holder.scriptName = (TextView)v.findViewById(R.id.script_name);
            holder.scriptDescription = (TextView)v.findViewById(R.id.script_description);

            v.setTag(holder);

            return v;
        }

        private void bindView(final View v, final Sandbox item) {
            final Holder holder = (Holder) v.getTag();
            holder.name.setText(item.getName());
            holder.scriptName.setText(item.getScriptName());
            holder.scriptDescription.setText(item.getScriptDescription());
        }
    }

    static class ScriptLoader extends AsyncTaskLoader<List<Sandbox>> {
        private static final String TAG = "Scriptloader";
        private final File mDir;

        public ScriptLoader(final Context context, final File dir) {
            super(context);
            mDir = dir;
        }

        @Override
        public List<Sandbox> loadInBackground() {
            final List<Sandbox> rv = new ArrayList<Sandbox>();

            Log.d(TAG, "loading scripts from %s", mDir.getAbsolutePath());

            for (final File f : mDir.listFiles()) {
                if (f.isFile() && f.getName().endsWith(".js")) {
                    Log.d(TAG, "loading script %s", f.getAbsolutePath());

                    final Sandbox s = new Sandbox(f.getName());
                    s.define("context", getContext());

                    try {
                        s.evaluate(f);
                        rv.add(s);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading script: " + f.getPath(), e);
                    }

                }
            }

            return rv;
        }
    }

}

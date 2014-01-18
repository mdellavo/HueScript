package org.quuux.huescript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.tools.shell.Global;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Sandbox implements Runnable  {

    private static final String TAG = Log.buildTag(Sandbox.class);

    private final File mPath;
    private final File mModulesPath;

    private Global mScope = new Global();
    private Require mRequire;
    private Scriptable mExports;

    public Sandbox(final File path, final File modulesPath) {
        mPath = path;
        mModulesPath = modulesPath;
    }

    public String getName() {
        final String name = mPath.getName();
        return name.substring(0, name.length() - 3);
    }

    public String getScriptName() {
        return (String) mExports.get("name", mExports);
    }


    public String getScriptDescription() {
        return (String) mExports.get("description", mExports);
    }

    public boolean run(final android.content.Context context) {
        try {
            ContextFactory.getGlobal().call(new ContextAction() {
                @Override
                public Object run(final Context cx) {
                    final Function func = (Function) mExports.get("main", mExports);
                    func.call(cx, mScope, mScope, new Object[]{ context });
                    return null;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "error running script", e);
            return false;
        }

        return true;
    }

    private void init(final Context context) {
        if (mRequire == null) {
            mScope.initStandardObjects(context, false);

            final WrapFactory wrapFactory = context.getWrapFactory();
            wrapFactory.setJavaPrimitiveWrap(false);

            context.setLanguageVersion(170);
            context.setOptimizationLevel(-1);

            List<String> paths = Arrays.asList(mPath.getParent(), mModulesPath.getAbsolutePath());
            mRequire = mScope.installRequire(context, paths, false);
        }
    }

    public void evaluate() {

        ContextFactory.getGlobal().call(new ContextAction() {
            @Override
            public Object run(final Context context) {

                init(context);
                mExports = mRequire.requireMain(context, getName());
                return null;
            }
        });
    }

    @Override
    public void run() {
        evaluate();
    }

}

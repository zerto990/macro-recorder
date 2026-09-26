package com.macrorecorder.app;

import android.util.Log;
import java.io.*;

public class RootCommandExecutor {
    private static final String TAG = "RootCmd";
    private Process rootProcess;
    private DataOutputStream out;
    private boolean rootAvailable = false;

    public boolean checkRoot() {
        try {
            Process p = Runtime.getRuntime().exec("su -c id");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.waitFor(); r.close();
            rootAvailable = line != null && line.contains("uid=0");
            return rootAvailable;
        } catch (Exception e) { rootAvailable = false; return false; }
    }

    public boolean initShell() {
        try {
            rootProcess = Runtime.getRuntime().exec("su");
            out = new DataOutputStream(rootProcess.getOutputStream());
            rootAvailable = true;
            return true;
        } catch (IOException e) { return false; }
    }

    public void exec(String cmd) {
        if (out == null) initShell();
        try {
            if (out != null) { out.writeBytes(cmd + "\n"); out.flush(); }
        } catch (IOException e) { Log.e(TAG, "exec failed", e); }
    }

    public void tap(float x, float y) { exec(String.format("input tap %.0f %.0f", x, y)); }
    public void longPress(float x, float y, long d) { exec(String.format("input swipe %.0f %.0f %.0f %.0f %d", x, y, x, y, d)); }
    public void swipe(float x1, float y1, float x2, float y2, long d) { exec(String.format("input swipe %.0f %.0f %.0f %.0f %d", x1, y1, x2, y2, d)); }
    public void keyEvent(int code) { exec("input keyevent " + code); }
    public void text(String t) { exec("input text '" + t.replace(" ", "%s") + "'"); }

    public String execOutput(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String l; while ((l = r.readLine()) != null) sb.append(l).append("\n");
            p.waitFor(); r.close();
            return sb.toString().trim();
        } catch (Exception e) { return ""; }
    }

    public void destroy() {
        try { if (out != null) { out.writeBytes("exit\n"); out.flush(); out.close(); } if (rootProcess != null) rootProcess.destroy(); } catch (Exception ignored) {}
    }

    public boolean isRootAvailable() { return rootAvailable; }
}

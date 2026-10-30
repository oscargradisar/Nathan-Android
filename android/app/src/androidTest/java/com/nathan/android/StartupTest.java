package com.nathan.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.net.HttpURLConnection;
import java.net.URL;

@RunWith(AndroidJUnit4.class)
public class StartupTest {
    @Rule public ActivityTestRule<MainActivity> activity = new ActivityTestRule<>(MainActivity.class);
    @Test public void embeddedEngineStartsAndKeysRoundTrip() throws Exception {
        MainActivity app = activity.getActivity();
        long deadline = System.currentTimeMillis() + 90000;
        while (app.serverUrl == null && app.startupError == null && System.currentTimeMillis() < deadline) Thread.sleep(250);
        assertNull(app.startupError, app.startupError);
        assertNotNull("On-device Python engine did not start", app.serverUrl);
        String base = app.serverUrl.split("/#")[0];
        HttpURLConnection status = (HttpURLConnection) new URL(base + "/api/status").openConnection();
        assertEquals(200, status.getResponseCode()); status.disconnect();
        KeyVault.put("instrumentation-test", "not-a-provider-key");
        assertEquals("not-a-provider-key", KeyVault.get("instrumentation-test"));
        KeyVault.remove("instrumentation-test");
        assertNull(KeyVault.get("instrumentation-test"));
        HttpURLConnection blocked = (HttpURLConnection) new URL(base + "/api/runs").openConnection();
        blocked.setRequestMethod("POST"); blocked.setDoOutput(true);
        blocked.getOutputStream().write("{}".getBytes());
        assertEquals(403, blocked.getResponseCode()); blocked.disconnect();
    }
}

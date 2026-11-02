package app.aethertv.cinema.probe;

import android.app.Activity;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/** Temporary L1 contract verifier. Logs counts/booleans only, never titles/IDs/tokens. */
public class ProbeActivity extends Activity {
    private static ClipData previousClip;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String mode = getIntent().getStringExtra("probe_mode");
        if ("input".equals(mode) || "restore_input".equals(mode)) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if ("input".equals(mode)) {
                previousClip = clipboard.getPrimaryClip();
                clipboard.setPrimaryClip(ClipData.newPlainText("Cinema test input", getIntent().getStringExtra("probe_text")));
            } else if (previousClip != null) {
                clipboard.setPrimaryClip(previousClip);
                previousClip = null;
            } else if (android.os.Build.VERSION.SDK_INT >= 28) {
                clipboard.clearPrimaryClip();
            }
            finish();
            return;
        }
        try (Cursor c = getContentResolver().query(Uri.parse("content://app.aethertv.cinema.snapshot/v1/cards"), null,null,null,null)) {
            int count = c == null ? 0 : c.getCount();
            if (count > 24) throw new IllegalStateException("Unbounded snapshot");
            boolean art = false;
            boolean play = false;
            while (c != null && c.moveToNext()) {
                String uri = c.getString(c.getColumnIndexOrThrow("art_uri"));
                if (uri != null && !art) {
                    try (ParcelFileDescriptor p = getContentResolver().openFileDescriptor(Uri.parse(uri),"r")) {
                        if (p == null || p.getStatSize() > 65536) throw new IllegalStateException("Art bound");
                        art = true;
                    }
                }
                if ("film-1".equals(c.getString(c.getColumnIndexOrThrow("item_id"))) && "play".equals(getIntent().getStringExtra("probe_mode"))) {
                    Intent i = new Intent("app.aethertv.cinema.PLAY_ITEM").setClassName("app.aethertv.cinema","app.orchard.tv.cinema.PlayItemActivity")
                        .putExtra("contract_version",1).putExtra("account_id",c.getString(c.getColumnIndexOrThrow("account_id")))
                        .putExtra("item_id","film-1").putExtra("position_ticks",100000000L);
                    startActivity(i);play=true;break;
                }
            }
            Log.i("CinemaProbe","PASS rows="+count+" bounded_art="+art+" play_dispatched="+play);
        } catch (Exception e) { Log.e("CinemaProbe","FAIL "+e.getClass().getSimpleName()); }
        finish();
    }
}

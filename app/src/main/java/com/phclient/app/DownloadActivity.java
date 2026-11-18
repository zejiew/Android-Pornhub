package com.phclient.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 下载列表：显示记录，点击用系统播放器打开本地文件。 */
public class DownloadActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackgroundColor(getColor(R.color.bg));
        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (14 * density);
        list.setPadding(pad, pad, pad, pad);
        scroll.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        List<DownloadStore.Item> items = DownloadStore.get(this).list();
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_downloads);
            empty.setTextColor(getColor(R.color.text_dim));
            empty.setPadding(pad, pad * 2, pad, pad);
            list.addView(empty);
            return;
        }

        SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

        for (DownloadStore.Item it : items) {
            TextView tv = new TextView(this);
            String time = fmt.format(new Date(it.time));
            tv.setText(it.name + "\n" + time);
            tv.setTextSize(14f);
            tv.setTextColor(getColor(R.color.text));
            tv.setPadding(pad, pad, pad, pad);
            tv.setBackgroundResource(R.drawable.bg_button);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = pad;
            tv.setLayoutParams(lp);

            tv.setOnClickListener(v -> play(it.name));

            tv.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setItems(new String[]{getString(R.string.delete_record),
                                getString(R.string.delete_file)}, (d, w) -> {
                            if (w == 0) {
                                DownloadStore.get(this).remove(it.name);
                            } else {
                                new File(DownloadStore.dir(), it.name).delete();
                                DownloadStore.get(this).remove(it.name);
                            }
                            recreate();
                        })
                        .show();
                return true;
            });

            list.addView(tv);
        }
    }

    private void play(String name) {
        File f = new File(DownloadStore.dir(), name);
        if (!f.exists()) {
            Toast.makeText(this, R.string.file_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        // Android 7+ 必须用 content:// URI，直接传 file:// 会抛 FileUriExposedException
        Uri uri = SimpleFileProvider.uriFor(this, f);
        i.setDataAndType(uri, "video/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(this, R.string.no_player, Toast.LENGTH_SHORT).show();
        }
    }
}

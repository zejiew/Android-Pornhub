package com.phclient.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * 极简 FileProvider 替代实现（零第三方依赖）。
 *
 * Android 7.0 起禁止对外暴露 file:// URI，打开下载好的视频必须用 content:// URI。
 * 这里只暴露 DownloadStore.dir() 这一个目录内的文件，且只读。
 */
public class SimpleFileProvider extends ContentProvider {

    public static final String AUTHORITY_SUFFIX = ".fileprovider";

    /** 用文件路径构造一个可授予权限的 content:// URI */
    public static Uri uriFor(android.content.Context ctx, File file) {
        String authority = ctx.getPackageName() + AUTHORITY_SUFFIX;
        return new Uri.Builder()
                .scheme("content")
                .authority(authority)
                .appendPath(file.getName())
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    /** 把 URI 映射回真实文件，并做目录越界校验（防 ../ 穿越） */
    private File resolve(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.isEmpty() || name.contains("..") || name.contains("/")) {
            throw new FileNotFoundException("非法文件名: " + name);
        }
        File base = DownloadStore.dir().getAbsoluteFile();
        File target = new File(base, name).getAbsoluteFile();
        try {
            if (!target.getCanonicalPath().startsWith(base.getCanonicalPath() + File.separator)) {
                throw new FileNotFoundException("越界访问: " + name);
            }
        } catch (java.io.IOException e) {
            throw new FileNotFoundException("路径解析失败: " + name);
        }
        if (!target.exists()) {
            throw new FileNotFoundException("文件不存在: " + name);
        }
        return target;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = resolve(uri);
        // 只允许只读，忽略调用方传入的写模式
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        try {
            File f = resolve(uri);
            String[] cols = projection != null ? projection
                    : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
            MatrixCursor cursor = new MatrixCursor(cols, 1);
            Object[] row = new Object[cols.length];
            for (int i = 0; i < cols.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) {
                    row[i] = f.getName();
                } else if (OpenableColumns.SIZE.equals(cols[i])) {
                    row[i] = f.length();
                } else {
                    row[i] = null;
                }
            }
            cursor.addRow(row);
            return cursor;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name == null) return "application/octet-stream";
        String lower = name.toLowerCase(java.util.Locale.US);
        if (lower.endsWith(".mp4") || lower.endsWith(".m4v")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".mkv")) return "video/x-matroska";
        if (lower.endsWith(".ts")) return "video/mp2t";
        return "video/*";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("只读");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("只读");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("只读");
    }
}

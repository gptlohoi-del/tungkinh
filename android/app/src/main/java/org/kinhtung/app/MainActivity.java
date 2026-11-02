package org.kinhtung.app;

import android.app.Activity;
import android.app.PrintManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaScannerConnection;
import android.webkit.WebViewDatabase;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * KINH TỤNG: một lớp bọc WebView cho site GitHub Pages.
 * OTA ở đây là cập nhật HTML/JS/nội dung kinh qua Service Worker của website,
 * không phải cập nhật gói Android APK.
 */
public final class MainActivity extends Activity {
    private WebView webView;
    private LinearLayout offlinePanel;
    private ProgressBar progressBar;
    private boolean mainPageFailed;
    private static final String PRINT_SCHEME = "kinhtung";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(255, 251, 235));

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); // Website gốc dùng JavaScript để chọn bài kinh.
        settings.setDomStorageEnabled(true); // Cấu hình và bộ tụng lưu trong localStorage.
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (PRINT_SCHEME.equalsIgnoreCase(url.getScheme()) && "print".equals(url.getHost())) {
                    startPrinting();
                    return true;
                }
                if (isSiteUrl(url)) return false;
                openExternally(url);
                return true;
            }

            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (isSiteUrl(Uri.parse(url))) {
                    mainPageFailed = false;
                    offlinePanel.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    mainPageFailed = true;
                    offlinePanel.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                if (!mainPageFailed && isSiteUrl(Uri.parse(url))) {
                    // Nút In trong website dùng window.print(); chuyển thành Android Print / Lưu PDF.
                    // Không dùng addJavascriptInterface để tránh lộ cầu nối Java cho mã web từ xa.
                    view.evaluateJavascript("window.print = function(){window.location.href='kinhtung://print';};", null);
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (url != null && url.startsWith("data:image/png;base64,")) {
                saveQrImage(url);
            } else if (url != null && url.startsWith("https://")) {
                openExternally(Uri.parse(url));
            } else {
                Toast.makeText(this, "Không hỗ trợ kiểu tệp này", Toast.LENGTH_SHORT).show();
            }
        });

        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP);
        root.addView(progressBar, barParams);

        offlinePanel = createOfflinePanel();
        offlinePanel.setVisibility(View.GONE);
        root.addView(offlinePanel, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            // WebView giữ lại trạng thái trang khi hệ thống huỷ Activity.
        } else {
            webView.loadUrl(BuildConfig.SITE_URL);
        }
    }

    private boolean isSiteUrl(Uri target) {
        Uri base = Uri.parse(BuildConfig.SITE_URL);
        String origin = target.getScheme();
        if (!"https".equalsIgnoreCase(origin)) return false;
        if (target.getHost() == null || !target.getHost().equalsIgnoreCase(base.getHost())) return false;
        int basePort = base.getPort() < 0 ? 443 : base.getPort();
        int targetPort = target.getPort() < 0 ? 443 : target.getPort();
        if (basePort != targetPort) return false;
        String prefix = base.getPath();
        if (prefix == null || prefix.isEmpty()) prefix = "/";
        if (!prefix.endsWith("/")) prefix += "/";
        return target.getPath() != null && target.getPath().startsWith(prefix);
    }

    private void openExternally(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return;
        if (!(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http")
                || scheme.equalsIgnoreCase("mailto") || scheme.equalsIgnoreCase("tel"))) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, "Không tìm thấy ứng dụng để mở liên kết", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout createOfflinePanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(28), dp(24), dp(28), dp(24));
        panel.setBackgroundColor(Color.rgb(255, 251, 235));

        TextView title = new TextView(this);
        title.setText(R.string.offline_title);
        title.setTextSize(23);
        title.setTextColor(Color.rgb(120, 53, 15));
        title.setGravity(Gravity.CENTER);
        panel.addView(title);

        TextView help = new TextView(this);
        help.setText(R.string.offline_help);
        help.setTextSize(16);
        help.setTextColor(Color.rgb(69, 26, 3));
        help.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams helpLp = new LinearLayout.LayoutParams(-1, -2);
        helpLp.topMargin = dp(16);
        panel.addView(help, helpLp);

        Button retry = new Button(this);
        retry.setText(R.string.retry);
        retry.setOnClickListener(v -> webView.loadUrl(BuildConfig.SITE_URL));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-2, -2);
        btnLp.topMargin = dp(20);
        panel.addView(retry, btnLp);
        return panel;
    }

    private void startPrinting() {
        try {
            PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            if (pm != null) {
                pm.print("KINH TỤNG", webView.createPrintDocumentAdapter("KINH TỤNG"), null);
            }
        } catch (RuntimeException exception) {
            Toast.makeText(this, "Không mở được chức năng in", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQrImage(String url) {
        try {
            byte[] bytes = Base64.decode(url.substring("data:image/png;base64,".length()), Base64.DEFAULT);
            String name = "KINH-TUNG-QR-" + System.currentTimeMillis() + ".png";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KinhTung");
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IllegalStateException("Không tạo được ảnh");
                try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                    if (output == null) throw new IllegalStateException("Không ghi được ảnh");
                    output.write(bytes);
                }
            } else {
                File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (dir == null) throw new IllegalStateException("Không có bộ nhớ");
                if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Không tạo được thư mục");
                File file = new File(dir, name);
                try (FileOutputStream output = new FileOutputStream(file)) {
                    output.write(bytes);
                }
                MediaScannerConnection.scanFile(this, new String[] {file.getAbsolutePath()},
                        new String[] {"image/png"}, null);
            }
            Toast.makeText(this, "Đã lưu mã QR vào Ảnh", Toast.LENGTH_LONG).show();
        } catch (Exception exception) {
            Toast.makeText(this, "Không lưu được mã QR", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return (int) (getResources().getDisplayMetrics().density * value + 0.5f);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}

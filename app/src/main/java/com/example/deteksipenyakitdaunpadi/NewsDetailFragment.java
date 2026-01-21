package com.example.deteksipenyakitdaunpadi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class NewsDetailFragment extends Fragment {

    private static final String ARG_URL = "url";

    private WebView webView;
    private ProgressBar progressWebLoading;

    private void setWebLoading(boolean show) {
        if (progressWebLoading != null) {
            progressWebLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public static NewsDetailFragment newInstance(String url) {
        NewsDetailFragment fragment = new NewsDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_news_detail, container, false);
        webView = root.findViewById(R.id.webView);
        progressWebLoading = root.findViewById(R.id.progressWebLoading);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                setWebLoading(true);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                setWebLoading(false);
                super.onPageCommitVisible(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                setWebLoading(false);
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Let WebView handle navigation to avoid duplicating history entries.
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                setWebLoading(false);
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                setWebLoading(false);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });

        String url = getArguments() != null ? getArguments().getString(ARG_URL, "") : "";
        if (url != null && !url.isEmpty()) {
            setWebLoading(true);
            webView.loadUrl(url);
        } else {
            setWebLoading(false);
        }

        return root;
    }

    public boolean handleBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        progressWebLoading = null;
        super.onDestroyView();
    }
}

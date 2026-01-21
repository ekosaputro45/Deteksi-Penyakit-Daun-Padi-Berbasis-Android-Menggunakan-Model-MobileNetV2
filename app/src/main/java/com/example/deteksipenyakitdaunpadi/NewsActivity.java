package com.example.deteksipenyakitdaunpadi;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsActivity extends AppCompatActivity implements NewsAdapter.OnItemClickListener {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AiNewsRssClient rssClient = new AiNewsRssClient();

    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressLoading;
    private LinearLayout llEmptyState;
    private LinearLayout llErrorState;
    private TextView tvErrorText;
    private Button btnRetry;
    private FrameLayout newsDetailContainer;
    private long lastExitDetailAttemptMs = 0L;

    private NewsAdapter adapter;
    private final List<NewsItem> allItems = new ArrayList<>();
    private final List<NewsItem> visibleItems = new ArrayList<>();
    private String currentKeyword = "";
    private String currentSearchQuery = "";
    private boolean isDetailVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            View bottomNavCard = v.findViewById(R.id.bottomNavCard);
            if (bottomNavCard != null) {
                int extraLiftPx = Math.round(8f * getResources().getDisplayMetrics().density);
                android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) bottomNavCard.getLayoutParams();
                lp.bottomMargin = bottomInset + extraLiftPx;
                bottomNavCard.setLayoutParams(lp);
            }

            View bottomContainer = v.findViewById(R.id.llBottomNavContainer);
            if (bottomContainer != null) {
                bottomContainer.setPadding(0, 0, 0, 0);
            }
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbarNews);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressLoading = findViewById(R.id.progressLoading);
        llEmptyState = findViewById(R.id.llEmptyState);
        llErrorState = findViewById(R.id.llErrorState);
        tvErrorText = findViewById(R.id.tvErrorText);
        btnRetry = findViewById(R.id.btnRetry);
        newsDetailContainer = findViewById(R.id.newsDetailContainer);

        RecyclerView rvNews = findViewById(R.id.rvNews);
        rvNews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NewsAdapter(this, visibleItems, this);
        rvNews.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> loadNews(true));
        btnRetry.setOnClickListener(v -> loadNews(true));

        setupBottomNavigation();
        setActiveNavItem(R.id.llNavCosmetologist);

        // Initial load
        loadNews(false);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (handleInAppBack()) return;
                finish();
            }
        });
    }

    private boolean handleInAppBack() {
        if (newsDetailContainer == null || newsDetailContainer.getVisibility() != View.VISIBLE) {
            return false;
        }

        Fragment f = getSupportFragmentManager().findFragmentById(R.id.newsDetailContainer);
        if (f instanceof NewsDetailFragment) {
            if (((NewsDetailFragment) f).handleBackPressed()) {
                return true;
            }
        }

        long now = SystemClock.elapsedRealtime();
        if (now - lastExitDetailAttemptMs < 2000L) {
            lastExitDetailAttemptMs = 0L;
            getSupportFragmentManager().popBackStack();
            setDetailVisible(false);
        } else {
            lastExitDetailAttemptMs = now;
            Toast.makeText(this, "Tekan sekali lagi untuk kembali ke daftar berita", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void openNewsDetail(String url) {
        if (url == null || url.trim().isEmpty()) return;

        lastExitDetailAttemptMs = 0L;
        setDetailVisible(true);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.newsDetailContainer, NewsDetailFragment.newInstance(url), "news_detail")
                .addToBackStack("news_detail")
                .commit();
    }

    private void setDetailVisible(boolean show) {
        isDetailVisible = show;
        if (newsDetailContainer != null) {
            newsDetailContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        swipeRefresh.setVisibility(show ? View.GONE : View.VISIBLE);
        invalidateOptionsMenu();
        // Keep state views hidden while detail is open
        if (show) {
            progressLoading.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.GONE);
            llErrorState.setVisibility(View.GONE);
        }
    }

    private void loadNews(boolean userInitiated) {
        showLoading(true);
        showError(false, null);
        showEmpty(false);

        // Default keyword set per spec
        String keywordQuery = (currentKeyword == null || currentKeyword.isEmpty())
                ? "pertanian padi OR beras OR \"rice farming\" OR \"panen padi\" OR \"irigasi sawah\""
                : currentKeyword;

        executor.execute(() -> {
            try {
                AiNewsRssClient.Result result = rssClient.fetch(keywordQuery);
                List<NewsItem> fetched = result.items != null ? result.items : new ArrayList<>();

                runOnUiThread(() -> {
                    allItems.clear();
                    allItems.addAll(fetched);
                    applySearchFilter(currentSearchQuery);
                    showLoading(false);
                    if (visibleItems.isEmpty()) {
                        showEmpty(true);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(true, e.getMessage());
                });
            }
        });
    }

    private void applySearchFilter(String query) {
        currentSearchQuery = query != null ? query : "";
        String q = currentSearchQuery.trim().toLowerCase();

        visibleItems.clear();
        if (q.isEmpty()) {
            visibleItems.addAll(allItems);
        } else {
            for (NewsItem item : allItems) {
                String hay = (safe(item.title) + " " + safe(item.summary) + " " + safe(item.source)).toLowerCase();
                if (hay.contains(q)) {
                    visibleItems.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();

        if (visibleItems.isEmpty() && !allItems.isEmpty()) {
            showEmpty(true);
            TextView tvEmptyHint = findViewById(R.id.tvEmptyHint);
            if (tvEmptyHint != null) {
                tvEmptyHint.setText("Tidak ada hasil untuk \"" + currentSearchQuery + "\".");
            }
        } else {
            showEmpty(false);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private void showLoading(boolean show) {
        progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void showEmpty(boolean show) {
        llEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(boolean show, String message) {
        llErrorState.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && tvErrorText != null) {
            tvErrorText.setText(message == null || message.isEmpty() ? "Gagal memuat berita" : ("Gagal memuat berita: " + message));
        }
    }

    @Override
    public void onItemClick(NewsItem item) {
        if (item.url == null || item.url.isEmpty()) return;
        openNewsDetail(item.url);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_news, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("Cari: padi, beras, irigasi, panen...");
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        applySearchFilter(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        applySearchFilter(newText);
                        return true;
                    }
                });
            }
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            boolean showSearch = !isDetailVisible;
            if (!showSearch && searchItem.isActionViewExpanded()) {
                searchItem.collapseActionView();
            }
            searchItem.setVisible(showSearch);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (handleInAppBack()) return true;
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBottomNavigation() {
        LinearLayout llNavHome = findViewById(R.id.llNavHome);
        LinearLayout llNavCosmetologist = findViewById(R.id.llNavCosmetologist);
        LinearLayout llNavHistory = findViewById(R.id.llNavHistory);
        LinearLayout llNavProfile = findViewById(R.id.llNavProfile);

        if (llNavHome != null) {
            llNavHome.setOnClickListener(v -> {
                setActiveNavItem(R.id.llNavHome);
                Intent intent = new Intent(NewsActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (llNavCosmetologist != null) {
            llNavCosmetologist.setOnClickListener(v -> setActiveNavItem(R.id.llNavCosmetologist));
        }

        if (llNavHistory != null) {
            llNavHistory.setOnClickListener(v -> {
                setActiveNavItem(R.id.llNavHistory);
                startActivity(new Intent(NewsActivity.this, HistoryActivity.class));
                finish();
            });
        }

        if (llNavProfile != null) {
            llNavProfile.setOnClickListener(v -> {
                setActiveNavItem(R.id.llNavProfile);
                startActivity(new Intent(NewsActivity.this, ProfileActivity.class));
                finish();
            });
        }
    }

    private void setActiveNavItem(int activeItemId) {
        setNavItemState(R.id.llNavHome, false);
        setNavItemState(R.id.llNavCosmetologist, false);
        setNavItemState(R.id.llNavHistory, false);
        setNavItemState(R.id.llNavProfile, false);
        setNavItemState(activeItemId, true);
    }

    private void setNavItemState(int itemId, boolean isActive) {
        LinearLayout item = findViewById(itemId);
        if (item == null) return;

        android.widget.ImageView icon = null;
        android.widget.TextView text = null;

        if (itemId == R.id.llNavHome) {
            icon = findViewById(R.id.ivNavHome);
            text = findViewById(R.id.tvNavHome);
        } else if (itemId == R.id.llNavCosmetologist) {
            icon = findViewById(R.id.ivNavCosmetologist);
            text = findViewById(R.id.tvNavCosmetologist);
        } else if (itemId == R.id.llNavHistory) {
            icon = findViewById(R.id.ivNavHistory);
            text = findViewById(R.id.tvNavHistory);
        } else if (itemId == R.id.llNavProfile) {
            icon = findViewById(R.id.ivNavProfile);
            text = findViewById(R.id.tvNavProfile);
        }

        if (icon != null) {
            icon.setColorFilter(isActive ? Color.parseColor("#F05A7E") : Color.parseColor("#9CA3AF"));
        }
        if (text != null) {
            text.setTextColor(isActive ? Color.parseColor("#F05A7E") : Color.parseColor("#9CA3AF"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}

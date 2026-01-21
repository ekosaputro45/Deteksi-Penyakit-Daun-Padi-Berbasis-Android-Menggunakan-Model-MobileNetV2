package com.example.deteksipenyakitdaunpadi;

import android.text.Html;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal RSS client khusus fitur AI News (read-only).
 * Sumber: Google News RSS (query-based). Tidak mengubah arsitektur app.
 */
public class AiNewsRssClient {

    public static class Result {
        public final List<NewsItem> items;

        public Result(List<NewsItem> items) {
            this.items = items;
        }
    }

    public Result fetch(String keywordQuery) throws Exception {
        String q = (keywordQuery == null || keywordQuery.trim().isEmpty())
                ? "pertanian padi OR beras OR \"rice farming\" OR \"panen padi\" OR \"irigasi sawah\""
                : keywordQuery.trim();

        String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8.name());

        // Google News RSS: bahasa Indonesia
        String urlStr = "https://news.google.com/rss/search?q=" + encoded + "&hl=id&gl=ID&ceid=ID:id";
        URL url = new URL(urlStr);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "DeteksiPenyakitDaunPadi/1.0");

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP " + code);
        }

        try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
            return new Result(parseRss(in));
        } finally {
            connection.disconnect();
        }
    }

    private List<NewsItem> parseRss(InputStream in) throws Exception {
        List<NewsItem> items = new ArrayList<>();

        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(in, null);

        boolean inItem = false;
        String title = null;
        String link = null;
        String sourceUrl = null;
        String pubDate = null;
        String descriptionHtml = null;
        String imageUrl = null;

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                String prefix = parser.getPrefix();

                if ("item".equalsIgnoreCase(name)) {
                    inItem = true;
                    title = link = sourceUrl = pubDate = descriptionHtml = imageUrl = null;
                } else if (inItem) {
                    if ("title".equalsIgnoreCase(name)) {
                        title = safeText(parser);
                    } else if ("link".equalsIgnoreCase(name)) {
                        link = safeText(parser);
                    } else if ("source".equalsIgnoreCase(name)) {
                        // Google News provides publisher base URL here.
                        sourceUrl = parser.getAttributeValue(null, "url");
                        // Consume text so parser state stays correct.
                        safeText(parser);
                    } else if ("pubDate".equalsIgnoreCase(name)) {
                        pubDate = safeText(parser);
                    } else if ("description".equalsIgnoreCase(name)) {
                        descriptionHtml = safeText(parser);
                        if (imageUrl == null) {
                            imageUrl = extractImgSrc(descriptionHtml);
                        }
                    } else if ("enclosure".equalsIgnoreCase(name)) {
                        // Some feeds provide an image enclosure
                        String type = parser.getAttributeValue(null, "type");
                        String url = parser.getAttributeValue(null, "url");
                        if (imageUrl == null && url != null && (type == null || type.startsWith("image/"))) {
                            imageUrl = url;
                        }
                    } else if (("media".equalsIgnoreCase(prefix) && ("content".equalsIgnoreCase(name) || "thumbnail".equalsIgnoreCase(name)))
                            || ("content".equalsIgnoreCase(name) || "thumbnail".equalsIgnoreCase(name))
                    ) {
                        // media:content / media:thumbnail
                        String url = parser.getAttributeValue(null, "url");
                        if (imageUrl == null && url != null) {
                            imageUrl = url;
                        }
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                String name = parser.getName();
                if ("item".equalsIgnoreCase(name) && inItem) {
                    inItem = false;

                    String dateFormatted = formatPubDate(pubDate);
                    String cleanedTitle = cleanTitle(title);

                    // Keep the article URL as-is; Google News RSS links are often redirect-wrapped.
                    // For source display, use the publisher domain from <source url>.
                    String articleUrl = link != null ? link : "";
                    String sourceDomain = extractSourceDomain(sourceUrl);
                    if (sourceDomain.isEmpty()) {
                        sourceDomain = extractSourceDomain(articleUrl);
                    }

                    String summary = cleanSummary(descriptionHtml, cleanedTitle);

                    // Ensure every item has an image: use site favicon as a lightweight thumbnail fallback.
                    if ((imageUrl == null || imageUrl.isEmpty()) && !sourceDomain.isEmpty()) {
                        imageUrl = "https://www.google.com/s2/favicons?domain=" + sourceDomain + "&sz=128";
                    }

                    if (title != null && link != null) {
                        items.add(new NewsItem(
                                cleanedTitle,
                                sourceDomain,
                                dateFormatted,
                                summary,
                                articleUrl,
                                imageUrl
                        ));
                    }
                }
            }

            event = parser.next();
        }

        return items;
    }

    private static String safeText(XmlPullParser parser) throws Exception {
        String text = "";
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
            parser.nextTag();
        }
        return text != null ? text.trim() : "";
    }

    private static String formatPubDate(String pubDate) {
        if (pubDate == null) return "";
        try {
            // Example: Tue, 02 Jan 2026 03:04:05 GMT
            SimpleDateFormat inFmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date d = inFmt.parse(pubDate);
            if (d == null) return "";
            SimpleDateFormat outFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return outFmt.format(d);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String extractImgSrc(String html) {
        if (html == null) return null;
        // Very small heuristic for <img src="..."> from RSS description
        int imgIndex = html.indexOf("<img");
        if (imgIndex < 0) return null;
        int srcIndex = html.indexOf("src=", imgIndex);
        if (srcIndex < 0) return null;
        int firstQuote = html.indexOf('"', srcIndex);
        if (firstQuote < 0) return null;
        int secondQuote = html.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        String url = html.substring(firstQuote + 1, secondQuote);
        return url.isEmpty() ? null : url;
    }

    private static String cleanTitle(String rawTitle) {
        String t = htmlToPlainText(rawTitle);
        if (t.isEmpty()) return "";

        // Common Google News pattern: "Judul Berita - Sumber.com".
        // Remove trailing publisher only if the suffix looks like a domain-ish token.
        while (true) {
            int idx = t.lastIndexOf(" - ");
            if (idx <= 0 || idx >= t.length() - 3) break;
            String suffix = t.substring(idx + 3).trim();
            if (!looksLikePublisherToken(suffix)) break;
            t = t.substring(0, idx).trim();
        }

        while (true) {
            int idx = t.lastIndexOf(" | ");
            if (idx <= 0 || idx >= t.length() - 3) break;
            String suffix = t.substring(idx + 3).trim();
            if (!looksLikePublisherToken(suffix)) break;
            t = t.substring(0, idx).trim();
        }

        return t;
    }

    private static boolean looksLikePublisherToken(String s) {
        if (s == null) return false;
        String token = s.trim();
        if (token.isEmpty()) return false;
        if (token.length() > 40) return false;
        if (token.contains(" ")) return false;
        // Examples: kompas.com, cnnindonesia.com, tempo.co, pertanian.go.id
        return token.contains(".");
    }

    private static String extractSourceDomain(String url) {
        if (url == null) return "";
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return "";
            host = host.toLowerCase(Locale.US);
            if (host.startsWith("www.")) host = host.substring(4);
            if (host.startsWith("m.")) host = host.substring(2);
            return host;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String cleanSummary(String descriptionHtml, String cleanedTitle) {
        String text = htmlToPlainText(descriptionHtml);
        if (text.isEmpty()) return "";

        // Google News RSS description often contains only: anchor(title) + publisher name.
        // If we can't find a meaningful snippet beyond title/source, return empty to keep UI clean.
        if (descriptionHtml != null && descriptionHtml.toLowerCase(Locale.US).contains("target=\"_blank\"")) {
            // likely the "link + source" format
            String lower = text.toLowerCase(Locale.US).trim();
            if (cleanedTitle != null && !cleanedTitle.isEmpty() && lower.equals(cleanedTitle.toLowerCase(Locale.US))) {
                return "";
            }
        }

        // Remove duplicated title if it appears at the beginning.
        if (cleanedTitle != null && !cleanedTitle.isEmpty()) {
            String t = cleanedTitle.trim();
            if (text.startsWith(t)) {
                text = text.substring(t.length()).trim();
                if (text.startsWith("-")) text = text.substring(1).trim();
            }
        }

        // Remove trailing publisher token like " - kompas.com".
        int idx = text.lastIndexOf(" - ");
        if (idx > 0 && idx < text.length() - 3) {
            String suffix = text.substring(idx + 3).trim();
            if (looksLikePublisherToken(suffix)) {
                text = text.substring(0, idx).trim();
            }
        }

        // If the remaining text looks like only a publisher token, drop it.
        if (looksLikePublisherToken(text)) {
            return "";
        }

        return text;
    }

    private static String htmlToPlainText(String html) {
        if (html == null) return "";

        // MinSdk is 24, so we can use FROM_HTML_MODE_LEGACY.
        String text;
        try {
            text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } catch (Exception e) {
            // Fallback for any malformed HTML.
            text = html.replaceAll("<[^>]*>", " ");
        }

        text = text
                .replace("\u00A0", " ")
                .replace("&nbsp;", " ")
                .trim();

        // Collapse whitespace
        return text.replaceAll("\\s{2,}", " ");
    }
}

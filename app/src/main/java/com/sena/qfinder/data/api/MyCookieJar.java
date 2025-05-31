package com.sena.qfinder.data.api;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class MyCookieJar implements CookieJar {
    private final Map<String, List<Cookie>> cookieStore = new HashMap<>();
    private static final String TAG = "MyCookieJar";

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String host = url.host();
        List<Cookie> storedCookies = cookieStore.getOrDefault(host, new ArrayList<>());

        for (Cookie newCookie : cookies) {
            storedCookies.removeIf(cookie -> cookie.name().equals(newCookie.name()));
            storedCookies.add(newCookie);
            Log.d(TAG, "Saved cookie: " + newCookie.name() + " for " + host);
        }

        cookieStore.put(host, storedCookies);
        logAllCookies();
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookiesToSend = new ArrayList<>();
        String host = url.host();

        for (Map.Entry<String, List<Cookie>> entry : cookieStore.entrySet()) {
            for (Cookie cookie : entry.getValue()) {
                if (cookie.matches(url)) {
                    cookiesToSend.add(cookie);
                }
            }
        }

        Log.d(TAG, "Sending " + cookiesToSend.size() + " cookies for " + host);
        return cookiesToSend;
    }

    public void clear() {
        cookieStore.clear();
        Log.d(TAG, "All cookies cleared");
    }

    public void clearForDomain(String domain) {
        if (cookieStore.containsKey(domain)) {
            cookieStore.get(domain).clear();
            cookieStore.remove(domain);
            Log.d(TAG, "Cookies cleared for domain: " + domain);
        }
    }

    public Cookie getCookie(String domain, String name) {
        List<Cookie> cookies = cookieStore.get(domain);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.name().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    private void logAllCookies() {
        // Eliminamos la comprobación de BuildConfig.DEBUG
        // y dejamos solo el logging en modo debug
        for (Map.Entry<String, List<Cookie>> entry : cookieStore.entrySet()) {
            for (Cookie cookie : entry.getValue()) {
                Log.d(TAG, String.format(
                        "Host: %s | Cookie: %s=%s | Secure: %b | HttpOnly: %b | Expires: %d",
                        entry.getKey(),
                        cookie.name(),
                        cookie.value(),
                        cookie.secure(),
                        cookie.httpOnly(),
                        cookie.expiresAt()
                ));
            }
        }
    }
}
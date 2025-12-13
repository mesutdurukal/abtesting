package com.indeed.demo.proctor;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.StringProctorLoader;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class DefinitionManager {
    private static final Logger logger = LogManager.getLogger(DefinitionManager.class);

    private static final String DEFAULT_SPEC = "/com/indeed/demo/ProctorGroups.json";
    private static final int RELOAD_INTERVAL_SECONDS = 10;

    private boolean cacheDisabled;
    private Map<String, Proctor> proctorCache = Maps.newHashMap();
    private Map<String, String> lastContentHash = Maps.newHashMap();
    private Random random = new Random();
    private ScheduledExecutorService scheduler;
    private AtomicLong saltVersion = new AtomicLong(System.currentTimeMillis());

    public DefinitionManager() {
    }

    @PostConstruct
    public void startAutoReload() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Set<String> urls = proctorCache.keySet();
                for (String url : urls) {
                    System.out.println("Auto-reloading definition: " + url);
                    load(url, true);
                }
            } catch (Exception e) {
                logger.error("Error during auto-reload", e);
            }
        }, RELOAD_INTERVAL_SECONDS, RELOAD_INTERVAL_SECONDS, TimeUnit.SECONDS);
        System.out.println("Auto-reload scheduler started (every " + RELOAD_INTERVAL_SECONDS + " seconds)");
    }

    @PreDestroy
    public void stopAutoReload() {
        if (scheduler != null) {
            scheduler.shutdown();
            System.out.println("Auto-reload scheduler stopped");
        }
    }

    private void disableHttpCache(final String definitionUrl) {
        if (cacheDisabled) {
            return;
        }
        cacheDisabled = true;
        try {
            final URL u = new URL(definitionUrl);
            final URLConnection uc = u.openConnection();
            uc.setDefaultUseCaches(false);
            System.out.println("Set default use of caches to " + uc.getDefaultUseCaches());
        } catch (Exception e) {
            System.err.println("Failed to disable caching");
            e.printStackTrace(System.err);
        }
    }

    public String loadRawJson(String definitionUrl) {
        disableHttpCache(definitionUrl);
        try {
            HttpURLConnection.setFollowRedirects(true);
            URL url = new URL(definitionUrl + "?r=" + random.nextInt());
            try (InputStream is = url.openStream(); Scanner scanner = new Scanner(is, "UTF-8")) {
                return scanner.useDelimiter("\\A").next();
            }
        } catch (Exception e) {
            logger.error("Failed to load raw JSON from " + definitionUrl, e);
            return null;
        }
    }

    public Proctor load(String definitionUrl, boolean forceReload) {
        Proctor proctor = proctorCache.get(definitionUrl);
        if (proctor != null && !forceReload) {
            System.out.println("reusing cached " + definitionUrl);
            return proctor;
        }
        disableHttpCache(definitionUrl);
        try {
            HttpURLConnection.setFollowRedirects(true); // for demo purposes, allow Java to follow redirects
            
            // Fetch the JSON content
            URL url = new URL(definitionUrl + "?r=" + random.nextInt());
            String jsonContent;
            try (InputStream is = url.openStream(); Scanner scanner = new Scanner(is, "UTF-8")) {
                jsonContent = scanner.useDelimiter("\\A").next();
            }
            
            // Check if content changed - if so, bump salt version to force re-bucketing
            String contentHash = String.valueOf(jsonContent.hashCode());
            String previousHash = lastContentHash.get(definitionUrl);
            if (previousHash != null && !previousHash.equals(contentHash)) {
                saltVersion.set(System.currentTimeMillis());
                System.out.println("Definition content changed! New salt version: " + saltVersion.get());
            }
            lastContentHash.put(definitionUrl, contentHash);
            
            // Modify salt in JSON to include version - forces re-bucketing on changes
            jsonContent = jsonContent.replaceAll(
                "(\"salt\"\\s*:\\s*\"[^\"]+)(\")", 
                "$1_v" + saltVersion.get() + "$2"
            );
            
            ProctorSpecification spec = ProctorUtils.readSpecification(
                DefinitionManager.class.getResourceAsStream(DEFAULT_SPEC));
            
            // Load from modified JSON string using StringProctorLoader
            StringProctorLoader loader = new StringProctorLoader(spec, definitionUrl, jsonContent);
            proctor = loader.doLoad();
            
            System.out.println("loaded definition from " + definitionUrl + " (salt version: " + saltVersion.get() + ")");
            proctorCache.put(definitionUrl, proctor);
        } catch (Throwable t) {
            logger.error("Failed to load test spec/definition", t);
            t.printStackTrace();
        }
        return proctor;
    }
}

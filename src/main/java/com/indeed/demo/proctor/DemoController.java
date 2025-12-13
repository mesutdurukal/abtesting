package com.indeed.demo.proctor;

import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Suppliers;
import com.indeed.demo.ProctorGroups;
import com.indeed.demo.ProctorGroupsManager;
import com.indeed.proctor.common.*;
import com.indeed.proctor.common.model.*;

import com.google.common.collect.ImmutableMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.indeed.web.useragents.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/")
public class DemoController {

    private static final String USER_ID_COOKIE = "UserId";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String DEFAULT_DEFINITION = "https://gist.githubusercontent.com/mesutdurukal/343755729c7ddedee28b49f3c22d7917/raw";
    private static final String PLATFORM_DEFINITION = "https://gist.githubusercontent.com/mesutdurukal/bd424fa4cfc069010882791004beb9d8/raw";

    @Autowired
    protected DefinitionManager definitionManager;

    private ProctorGroups getProctorGroups(
            @Nonnull final HttpServletRequest request,
            @Nonnull final HttpServletResponse response,
            @Nonnull String userId,
            @Nonnull String definitionUrl,
            @Nullable UserAgent userAgent) {
        final Proctor proctor = definitionManager.load(definitionUrl, false);
        final ProctorGroupsManager groupsManager = new ProctorGroupsManager(Suppliers.ofInstance(proctor));
        final Identifiers identifiers = new Identifiers(TestType.USER, userId);
        final boolean allowForceGroups = true;
        final ProctorResult result = groupsManager.determineBuckets(
                request, response, identifiers, allowForceGroups, userAgent);
        final ProctorGroups groups = new ProctorGroups(result);
        return groups;
    }

    private String getGreetingFromDefinition(
            @Nonnull String userId,
            @Nonnull String definitionUrl) {
        try {
            String json = definitionManager.loadRawJson(definitionUrl);
            if (json == null) return "";
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode greetingTest = root.path("tests").path("greetingtst");
            if (greetingTest.isMissingNode()) return "";
            
            String salt = greetingTest.path("salt").asText("greetingtst");
            JsonNode buckets = greetingTest.path("buckets");
            JsonNode allocations = greetingTest.path("allocations").get(0).path("ranges");
            
            // Hash userId with salt to get bucket position (0.0 to 1.0)
            int hash = (userId + salt).hashCode();
            double position = (double) (hash & 0x7FFFFFFF) / Integer.MAX_VALUE;
            
            // Find bucket based on allocation ranges
            double cumulative = 0.0;
            int bucketValue = -1;
            for (JsonNode range : allocations) {
                cumulative += range.path("length").asDouble();
                if (position < cumulative) {
                    bucketValue = range.path("bucketValue").asInt();
                    break;
                }
            }
            
            // Find greeting for bucket
            for (JsonNode bucket : buckets) {
                if (bucket.path("value").asInt() == bucketValue) {
                    return bucket.path("payload").path("map").path("greeting").asText("");
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting greeting: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView handle(@Nonnull final HttpServletRequest request,
            @Nonnull final HttpServletResponse response,
            @Nullable @CookieValue(required = false, value = USER_ID_COOKIE) String userId,
            @Nullable @RequestHeader(required = false, value = USER_AGENT_HEADER) String userAgentHeader,
            @Nullable @RequestParam(required = false, value = "defn") String definitionUrl,
            @Nullable @RequestParam(required = false, value = "defn2") String definitionUrl2,
            @RequestParam(required = false, value = "mydevice") String mydevice) {
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            response.addCookie(new Cookie(USER_ID_COOKIE, userId));
        }
        final UserAgent userAgent = UserAgent.parseUserAgentStringSafely(userAgentHeader);
        System.out.println("User-Agent: " + userAgentHeader);
        System.out.println("isIOS: " + userAgent.isIOS() + ", isAndroid: " + userAgent.isAndroid() + ", isMobile: " + userAgent.isMobileDevice());
        String defn;
        if (mydevice != null) {
            defn = PLATFORM_DEFINITION;
        } else if (definitionUrl != null && !definitionUrl.isEmpty()) {
            defn = definitionUrl;
        } else {
            defn = DEFAULT_DEFINITION;
        }
        System.out.println("Using definition URL: " + defn);
        final ProctorGroups groups = getProctorGroups(request, response, userId, defn, userAgent);
        
        // Load second definition if provided (for greeting)
        String greeting = "";
        if (definitionUrl2 != null && !definitionUrl2.isEmpty()) {
            System.out.println("Using definition URL 2: " + definitionUrl2);
            greeting = getGreetingFromDefinition(userId, definitionUrl2);
            System.out.println("Greeting: " + greeting);
        }
        
        return new ModelAndView("demo", ImmutableMap.of("groups", groups, "greeting", greeting));
    }
}

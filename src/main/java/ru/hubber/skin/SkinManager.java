package ru.hubber.skin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mannequin;
import ru.hubber.HubberPlugin;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinManager {

    private final HubberPlugin plugin;
    private final Map<String, SkinResult> skinCache = new ConcurrentHashMap<>();

    public record SkinResult(String value, String signature) {
    }

    public SkinManager(HubberPlugin plugin) {
        this.plugin = plugin;
    }

    public void applySkin(Mannequin mannequin, String skinInput) {
        if (skinInput == null || skinInput.trim().isEmpty()
                || skinInput.equalsIgnoreCase("none")
                || skinInput.equalsIgnoreCase("default")) {
            return;
        }

        String target = skinInput.trim();

        // Check cache first
        SkinResult cached = skinCache.get(target.toLowerCase());
        if (cached != null) {
            applySkinToMannequin(mannequin, cached);
            return;
        }

        // Resolve asynchronously to prevent main-thread lag
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            SkinResult resolved = resolveSkin(target);
            if (resolved != null) {
                skinCache.put(target.toLowerCase(), resolved);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (mannequin.isValid()) {
                        applySkinToMannequin(mannequin, resolved);
                    }
                });
            } else {
                plugin.getLogger().warning("Could not resolve skin for input: '" + target + "'");
            }
        });
    }

    private void applySkinToMannequin(Mannequin mannequin, SkinResult result) {
        try {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "NPC");
            if (result.signature() != null && !result.signature().isEmpty()) {
                profile.setProperty(new ProfileProperty("textures", result.value(), result.signature()));
            } else {
                profile.setProperty(new ProfileProperty("textures", result.value()));
            }

            mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply profile to Mannequin: " + e.getMessage());
        }
    }

    private SkinResult resolveSkin(String input) {
        // 1. Try SkinsRestorer first (if present on server)
        SkinResult srResult = trySkinsRestorer(input);
        if (srResult != null) {
            return srResult;
        }

        // 2. Direct textures URL or 64-char texture hash
        if (input.startsWith("http://") || input.startsWith("https://") || input.matches("^[a-fA-F0-9]{64}$")) {
            return createFromUrl(input);
        }

        // 3. Fallback: Fetch directly from Mojang API (signed)
        SkinResult mojangResult = fetchFromMojang(input);
        if (mojangResult != null) {
            return mojangResult;
        }

        return null;
    }

    private SkinResult trySkinsRestorer(String input) {
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
                return null;
            }

            // 1. Try modern SkinsRestorer v15+ API
            try {
                Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
                Object api = providerClass.getMethod("get").invoke(null);
                if (api != null) {
                    Object skinStorage = api.getClass().getMethod("getSkinStorage").invoke(api);
                    if (skinStorage != null) {
                        // Method 1a: findOrCreateSkinData(String) - queries cache or downloads on demand
                        try {
                            Method findMethod = skinStorage.getClass().getMethod("findOrCreateSkinData", String.class);
                            Optional<?> opt = (Optional<?>) findMethod.invoke(skinStorage, input);
                            if (opt != null && opt.isPresent()) {
                                SkinResult res = extractFromSkinData(opt.get());
                                if (res != null) return res;
                            }
                        } catch (Throwable ignored) {
                        }

                        // Method 1b: getSkinData(String)
                        try {
                            Method getSkinDataMethod = skinStorage.getClass().getMethod("getSkinData", String.class);
                            Optional<?> opt = (Optional<?>) getSkinDataMethod.invoke(skinStorage, input);
                            if (opt != null && opt.isPresent()) {
                                SkinResult res = extractFromSkinData(opt.get());
                                if (res != null) return res;
                            }
                        } catch (Throwable ignored) {
                        }

                        // Method 1c: getSkin(String) returning SkinProperty
                        try {
                            Method getSkinMethod = skinStorage.getClass().getMethod("getSkin", String.class);
                            Optional<?> opt = (Optional<?>) getSkinMethod.invoke(skinStorage, input);
                            if (opt != null && opt.isPresent()) {
                                SkinResult res = extractFromProperty(opt.get());
                                if (res != null) return res;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }

            // 2. Fallback: Try legacy SkinsRestorer v14 API
            try {
                Class<?> legacyApiClass = Class.forName("net.skinsrestorer.api.SkinsRestorerAPI");
                Method getApiMethod = legacyApiClass.getMethod("getApi");
                Object legacyApi = getApiMethod.invoke(null);
                if (legacyApi != null) {
                    Method getSkinDataMethod = legacyApi.getClass().getMethod("getSkinData", String.class);
                    Object prop = getSkinDataMethod.invoke(legacyApi, input);
                    if (prop != null) {
                        SkinResult res = extractFromProperty(prop);
                        if (res != null) return res;
                    }
                }
            } catch (Throwable ignored) {
            }

        } catch (Throwable ignored) {
        }
        return null;
    }

    private SkinResult extractFromSkinData(Object data) {
        try {
            Object prop = data.getClass().getMethod("getProperty").invoke(data);
            if (prop != null) {
                return extractFromProperty(prop);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private SkinResult extractFromProperty(Object prop) {
        try {
            String value = (String) prop.getClass().getMethod("getValue").invoke(prop);
            String signature = (String) prop.getClass().getMethod("getSignature").invoke(prop);
            if (value != null && !value.isEmpty()) {
                return new SkinResult(value, signature);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private SkinResult createFromUrl(String urlOrHash) {
        String url = urlOrHash.trim();
        if (url.matches("^[a-fA-F0-9]{64}$")) {
            url = "http://textures.minecraft.net/texture/" + url;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return new SkinResult(base64, null);
    }

    private SkinResult fetchFromMojang(String username) {
        try {
            URI nameUri = URI.create("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection conn = (HttpURLConnection) nameUri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "Hubber-Minecraft-Plugin");

            if (conn.getResponseCode() != 200) {
                return null;
            }

            String jsonStr;
            try (InputStream in = conn.getInputStream()) {
                jsonStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
            if (!obj.has("id")) {
                return null;
            }
            String uuid = obj.get("id").getAsString();

            URI profileUri = URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            HttpURLConnection pConn = (HttpURLConnection) profileUri.toURL().openConnection();
            pConn.setRequestMethod("GET");
            pConn.setConnectTimeout(4000);
            pConn.setReadTimeout(4000);
            pConn.setRequestProperty("User-Agent", "Hubber-Minecraft-Plugin");

            if (pConn.getResponseCode() != 200) {
                return null;
            }

            String pJsonStr;
            try (InputStream in = pConn.getInputStream()) {
                pJsonStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            JsonObject pObj = JsonParser.parseString(pJsonStr).getAsJsonObject();
            if (!pObj.has("properties")) {
                return null;
            }

            JsonArray props = pObj.getAsJsonArray("properties");
            for (JsonElement el : props) {
                JsonObject prop = el.getAsJsonObject();
                if ("textures".equals(prop.get("name").getAsString())) {
                    String val = prop.get("value").getAsString();
                    String sig = prop.has("signature") ? prop.get("signature").getAsString() : null;
                    return new SkinResult(val, sig);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

package net.pixeldreamstudios.rpgsystems.client.title;

import net.minecraft.util.Formatting;

public final class TitleStyleUtil {
    private TitleStyleUtil() {}

    public record Parsed(
            String text, boolean rainbow, boolean wiggle, Integer baseRgb,
            Float rainbowSpeed, Float wiggleAmp, Float wiggleSpeed,
            int[] gradient,
            Float pulseSpeed,
            Float shakeAmp,
            Float bounceAmp, Float bounceSpeed,
            Float waveAmp,   Float waveSpeed,
            Float glitchIntensity
    ) {}

    public static Parsed parse(String raw) {
        boolean rainbow = containsToken(raw, "{rainbow}");
        boolean wiggle  = containsToken(raw, "{wiggle}");

        Float rainbowSpeed = readFloatParam(raw, "rainbow");
        Float wiggleAmp    = readKeyedParam(raw, "wiggle", "amp");
        Float wiggleSpeed  = readKeyedParam(raw, "wiggle", "speed");

        int[] gradient     = readGradient(raw);
        Float pulseSpeed   = readFloatParam(raw, "pulse");
        Float shakeAmp     = readFloatParam(raw, "shake");
        Integer hexColor   = readHexColor(raw);

        Float bounceAmp    = readKeyedParam(raw, "bounce", "amp");
        Float bounceSpeed  = readKeyedParam(raw, "bounce", "speed");

        Float waveAmp      = readKeyedParam(raw, "wave", "amp");
        Float waveSpeed    = readKeyedParam(raw, "wave", "speed");

        Float glitchIntensity = readKeyedParam(raw, "glitch", "intensity");

        String s = removeToken(removeToken(raw, "{rainbow}"), "{wiggle}").trim();
        s = stripDynamicTokens(s);

        Integer color = hexColor;
        StringBuilder out = new StringBuilder(s.length());

        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < s.length()) {
                char code = Character.toLowerCase(s.charAt(i + 1));
                if (code == 'r') { color = null; i += 2; continue; }
                Integer mapped = mapLegacyColor(code);
                if (mapped != null) { color = mapped; i += 2; continue; }
            }
            int cp = s.codePointAt(i);
            out.appendCodePoint(cp);
            i += Character.charCount(cp);
        }

        return new Parsed(out.toString(), rainbow, wiggle, color,
                rainbowSpeed, wiggleAmp, wiggleSpeed,
                gradient, pulseSpeed, shakeAmp,
                bounceAmp, bounceSpeed,
                waveAmp,   waveSpeed,
                glitchIntensity);
    }

    private static String tokenPayload(String s, String name) {
        String low = s.toLowerCase();
        String open = "{" + name.toLowerCase();
        int i = low.indexOf(open);
        if (i < 0) return null;
        int end = s.indexOf('}', i);
        if (end < 0) return null;
        String inside = s.substring(i + 1, end);
        int colon = inside.indexOf(':');
        return colon >= 0 ? inside.substring(colon + 1).trim() : "";
    }

    private static String stripDynamicTokens(String s) {
        String[] names = {"gradient","pulse","shake","color","rainbow","wiggle","outline","bounce","wave","glitch"};
        for (String n : names) {
            while (true) {
                String low = s.toLowerCase();
                int i = low.indexOf("{"+n);
                if (i < 0) break;
                int end = s.indexOf('}', i);
                if (end < 0) break;
                s = s.substring(0, i) + s.substring(end+1);
            }
        }
        return s;
    }

    private static int parseHex(String hex) {
        hex = hex.replace("#","").trim();
        return (int)Long.parseLong(hex, 16) & 0xFFFFFF;
    }

    private static Integer readHexColor(String s) {
        String p = tokenPayload(s, "color");
        if (p == null || p.isEmpty()) return null;
        try { return parseHex(p); } catch (Exception ignore) { return null; }
    }

    private static Float readFloatParam(String s, String name) {
        String p = tokenPayload(s, name);
        if (p == null || p.isEmpty()) return null;
        try { return Float.parseFloat(p); } catch (Exception ignore) { return null; }
    }

    private static Float readKeyedParam(String s, String name, String key) {
        String p = tokenPayload(s, name);
        if (p == null) return null;
        for (String kv : p.split(",")) {
            int eq = kv.indexOf('=');
            if (eq > 0 && key.equalsIgnoreCase(kv.substring(0, eq).trim())) {
                try { return Float.parseFloat(kv.substring(eq+1).trim()); } catch (Exception ignore) {}
            }
        }
        return null;
    }

    private static int[] readGradient(String s) {
        String p = tokenPayload(s, "gradient");
        if (p == null || p.isEmpty()) return null;
        String[] cols = p.split(",");
        int[] out = new int[cols.length];
        int n = 0;
        for (String c : cols) {
            try { out[n++] = parseHex(c); } catch (Exception ignore) {}
        }
        return n == 0 ? null : (n == out.length ? out : java.util.Arrays.copyOf(out, n));
    }

    public static int resolveOrWhite(Integer rgb) {
        return rgb != null ? rgb : 0xFFFFFF;
    }

    public static int rainbowRgb(long nowMs, int index) {
        float t = (nowMs % 1000000L) / 1000.0f;
        float hue = wrap01(t * 0.18f + index * 0.12f);
        return hsbToRgb(hue, 1.0f, 1.0f);
    }

    public static float wiggleYOffsetPx(long nowMs, int index, float amplitudePx) {
        float t = (nowMs % 1000000L) / 1000.0f;
        float w = (float) (Math.sin(t * 5.6f + index * 0.45f));
        return w * amplitudePx;
    }

    public static float bounceYOffsetPx(long nowMs, int index, float amplitudePx, float speed) {
        float t = (nowMs % 1000000L) / 1000.0f;
        float v = (float)Math.sin(t * speed + index * 0.55f);
        return Math.abs(v) * amplitudePx;
    }

    public static float waveXOffsetPx(long nowMs, int index, float amplitudePx, float speed) {
        float t = (nowMs % 1000000L) / 1000.0f;
        float v = (float)Math.sin(t * speed + index * 0.55f);
        return v * amplitudePx;
    }

    private static boolean containsToken(String s, String token) {
        return s.toLowerCase().contains(token.toLowerCase());
    }

    private static String removeToken(String s, String token) {
        return s.replace(token, "").replace(token.toUpperCase(), "");
    }

    private static Integer mapLegacyColor(char code) {
        Formatting f = switch (code) {
            case '0' -> Formatting.BLACK;
            case '1' -> Formatting.DARK_BLUE;
            case '2' -> Formatting.DARK_GREEN;
            case '3' -> Formatting.DARK_AQUA;
            case '4' -> Formatting.DARK_RED;
            case '5' -> Formatting.DARK_PURPLE;
            case '6' -> Formatting.GOLD;
            case '7' -> Formatting.GRAY;
            case '8' -> Formatting.DARK_GRAY;
            case '9' -> Formatting.BLUE;
            case 'a' -> Formatting.GREEN;
            case 'b' -> Formatting.AQUA;
            case 'c' -> Formatting.RED;
            case 'd' -> Formatting.LIGHT_PURPLE;
            case 'e' -> Formatting.YELLOW;
            case 'f' -> Formatting.WHITE;
            default -> null;
        };
        if (f == null) return null;
        Integer v = f.getColorValue();
        return v == null ? null : (v & 0xFFFFFF);
    }

    private static float wrap01(float v) {
        float r = v % 1.0f;
        return r < 0f ? r + 1.0f : r;
    }

    private static int hsbToRgb(float h, float s, float b) {
        h = wrap01(h);
        s = clamp01(s);
        b = clamp01(b);

        float r = 0, g = 0, bl = 0;
        int i = (int) (h * 6.0f);
        float f = h * 6.0f - i;
        float p = b * (1.0f - s);
        float q = b * (1.0f - s * f);
        float t = b * (1.0f - s * (1.0f - f));
        switch (i % 6) {
            case 0 -> { r = b; g = t; bl = p; }
            case 1 -> { r = q; g = b; bl = p; }
            case 2 -> { r = p; g = b; bl = t; }
            case 3 -> { r = p; g = q; bl = b; }
            case 4 -> { r = t; g = p; bl = b; }
            case 5 -> { r = b; g = p; bl = q; }
        }
        int ri = Math.round(r * 255f) & 0xFF;
        int gi = Math.round(g * 255f) & 0xFF;
        int bi = Math.round(bl * 255f) & 0xFF;
        return (ri << 16) | (gi << 8) | bi;
    }

    public static int gradientRgb(int index, int length, int[] cols) {
        if (cols == null || cols.length == 0) return 0xFFFFFF;
        if (cols.length == 1) return cols[0];
        float t = (length <= 1) ? 0f : (index / (float)(length - 1));
        float x = t * (cols.length - 1);
        int i = Math.max(0, Math.min(cols.length - 2, (int)Math.floor(x)));
        float f = x - i;
        int a = cols[i], b = cols[i+1];
        int ar=(a>>16)&255, ag=(a>>8)&255, ab=a&255;
        int br=(b>>16)&255, bg=(b>>8)&255, bb=b&255;
        int r=(int)(ar*(1-f)+br*f), g=(int)(ag*(1-f)+bg*f), bl=(int)(ab*(1-f)+bb*f);
        return (r<<16)|(g<<8)|bl;
    }

    public static int pulseRgb(long nowMs, int baseRgb, float speed) {
        float t = (nowMs % 1_000_000L) / 1000f;
        float k = 0.5f + 0.5f*(float)Math.sin(t * (float)(Math.PI*2) * speed);
        float mul = 0.6f + 0.4f*k;
        int r = Math.min(255, (int)(((baseRgb>>16)&255) * mul));
        int g = Math.min(255, (int)(((baseRgb>>8 )&255) * mul));
        int b = Math.min(255, (int)(((baseRgb    )&255) * mul));
        return (r<<16)|(g<<8)|b;
    }

    public static float shakeXOffsetPx(long nowMs, int index, float ampPx) {
        float t = (nowMs % 1_000_000L) / 1000f;
        return (float)(Math.sin(t*9.0 + index*0.9) * ampPx);
    }

    public static int rainbowRgb(long nowMs, int index, float speed) {
        float t = (nowMs % 1_000_000L) / 1000.0f;
        float hue = wrap01(t * speed + index * 0.12f);
        return hsbToRgb(hue, 1f, 1f);
    }

    public static boolean glitchActive(long nowMs, int index, float intensity) {
        float p = clamp01(intensity * 0.35f + 0.05f);
        int frame = (int)(nowMs / 50L);
        int h = fastHash(index * 374761393 + frame * 668265263);
        float r = frac01(h);
        return r < p;
    }

    public static float glitchJitterX(long nowMs, int index, float intensity) {
        if (intensity <= 0f) return 0f;
        int frame = (int)(nowMs / 50L);
        int h = fastHash(index * 915488749 + frame * 140294673);
        float mag = 1.0f + intensity * 2.0f;
        return (frac01(h) - 0.5f) * 2f * mag;
    }

    public static float glitchJitterY(long nowMs, int index, float intensity) {
        if (intensity <= 0f) return 0f;
        int frame = (int)(nowMs / 50L);
        int h = fastHash(index * 19990303 + frame * 636413622);
        float mag = 0.5f + intensity * 1.5f;
        return (frac01(h) - 0.5f) * 2f * mag;
    }

    public static int glitchTintRgb(long nowMs, int index, int baseRgb, float intensity) {
        if (intensity <= 0f) return baseRgb;
        int frame = (int)(nowMs / 50L);
        int h = fastHash(index * 1103515245 + frame * 12345);
        float hue = frac01(h * 3);
        int tint = hsbToRgb(hue, 1f, 1f);
        float a = 0.35f + 0.35f * clamp01(intensity);
        return lerpRgb(baseRgb, tint, a);
    }

    private static int lerpRgb(int a, int b, float t) {
        t = clamp01(t);
        int ar=(a>>16)&255, ag=(a>>8)&255, ab=a&255;
        int br=(b>>16)&255, bg=(b>>8)&255, bb=b&255;
        int r = (int)(ar*(1f-t) + br*t);
        int g = (int)(ag*(1f-t) + bg*t);
        int bl= (int)(ab*(1f-t) + bb*t);
        return (r<<16)|(g<<8)|bl;
    }

    private static int fastHash(int x) {
        x ^= (x >>> 16);
        x *= 0x7feb352d;
        x ^= (x >>> 15);
        x *= 0x846ca68b;
        x ^= (x >>> 16);
        return x;
    }

    private static float frac01(int h) {
        return ((h >>> 1) & 0x7FFFFFFF) / 2147483647f;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}

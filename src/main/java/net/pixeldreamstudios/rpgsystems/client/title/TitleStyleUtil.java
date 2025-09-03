package net.pixeldreamstudios.rpgsystems.client.title;

import net.minecraft.util.Formatting;

public final class TitleStyleUtil {
    private TitleStyleUtil() {}

    public record Parsed(
            String text, boolean rainbow, boolean wiggle, Integer baseRgb,
            Float rainbowSpeed, Float wiggleAmp, Float wiggleSpeed,
            int[] gradient,
            Float pulseSpeed,
            Integer outlineRgb,
            Integer outlinePx,
            Float shakeAmp
    ) {}

    public static Parsed parse(String raw) {
        boolean rainbow = containsToken(raw, "{rainbow}");
        boolean wiggle  = containsToken(raw, "{wiggle}");

        Float rainbowSpeed = readFloatParam(raw, "rainbow");
        Float wiggleAmp    = readKeyedParam(raw, "wiggle", "amp");
        Float wiggleSpeed  = readKeyedParam(raw, "wiggle", "speed");

        int[] gradient     = readGradient(raw);
        Float pulseSpeed   = readFloatParam(raw, "pulse");
        Integer outlineRgb = readColorParam(raw, "outline", 0);
        Integer outlinePx  = readIntParam(raw, "outline", 1);
        Float shakeAmp     = readFloatParam(raw, "shake");
        Integer hexColor   = readHexColor(raw);

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
                gradient, pulseSpeed, outlineRgb, outlinePx, shakeAmp);
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
        String[] names = {"gradient","pulse","outline","shake","color","rainbow","wiggle"};
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
    private static Integer readColorParam(String s, String name, int idx) {
        String p = tokenPayload(s, name);
        if (p == null || p.isEmpty()) return null;
        String[] parts = p.split(":");
        if (idx < parts.length && !parts[idx].isBlank()) {
            try { return parseHex(parts[idx]); } catch (Exception ignore) {}
        }
        return null;
    }
    private static Float readFloatParam(String s, String name) {
        String p = tokenPayload(s, name);
        if (p == null || p.isEmpty()) return null;
        try { return Float.parseFloat(p); } catch (Exception ignore) { return null; }
    }
    private static Integer readIntParam(String s, String name, int idx) {
        String p = tokenPayload(s, name);
        if (p == null || p.isEmpty()) return null;
        String[] parts = p.split(":");
        if (idx < parts.length && !parts[idx].isBlank()) {
            try { return Integer.parseInt(parts[idx]); } catch (Exception ignore) {}
        }
        return null;
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

    private static boolean containsToken(String s, String token) {
        return s.toLowerCase().contains(token.toLowerCase());
    }

    private static String removeToken(String s, String token) {
        return s.replace(token, "").replace(token.toUpperCase(), "");
    }

    private static Float toFormattingColor(Formatting fmt) {
        Integer v = fmt.getColorValue();
        return v == null ? null : v.floatValue();
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

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}

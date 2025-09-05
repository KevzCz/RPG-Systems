package net.pixeldreamstudios.rpgsystems.client.title;

import net.minecraft.util.Formatting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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

    public record Span(
            String text,
            boolean rainbow, Float rainbowSpeed,
            boolean wiggle,  Float wiggleAmp, Float wiggleSpeed,
            int[] gradient,
            Integer baseRgb,
            Float pulseSpeed,
            Float shakeAmp,
            Float bounceAmp, Float bounceSpeed,
            Float waveAmp,   Float waveSpeed,
            Float glitchIntensity
    ) {}

    private static final class Wiggle { final Float amp, speed; Wiggle(Float a, Float s){amp=a;speed=s;} }
    private static final class Wave   { final Float amp, speed; Wave  (Float a, Float s){amp=a;speed=s;} }
    private static final class Bounce { final Float amp, speed; Bounce(Float a, Float s){amp=a;speed=s;} }

    private static final class Stacks {
        final Deque<Float>  rainbow = new ArrayDeque<>();
        final Deque<Wiggle> wiggle  = new ArrayDeque<>();
        final Deque<int[]>  gradient= new ArrayDeque<>();
        final Deque<Integer>color   = new ArrayDeque<>();
        final Deque<Float>  pulse   = new ArrayDeque<>();
        final Deque<Float>  shake   = new ArrayDeque<>();
        final Deque<Bounce> bounce  = new ArrayDeque<>();
        final Deque<Wave>   wave    = new ArrayDeque<>();
        final Deque<Float>  glitch  = new ArrayDeque<>();

        void clearAll() {
            rainbow.clear(); wiggle.clear(); gradient.clear(); color.clear();
            pulse.clear(); shake.clear(); bounce.clear(); wave.clear(); glitch.clear();
        }

        Span toSpan(String text) {
            boolean rainbowOn = !rainbow.isEmpty();
            boolean wiggleOn  = !wiggle.isEmpty();
            Float  rainbowSpd = rainbowOn ? rainbow.peekLast() : null;

            Wiggle w  = wiggleOn ? wiggle.peekLast() : null;
            Bounce b  = bounce.isEmpty() ? null : bounce.peekLast();
            Wave   wa = wave.isEmpty()   ? null : wave.peekLast();

            return new Span(
                    text,
                    rainbowOn, rainbowSpd,
                    wiggleOn,  w == null ? null : w.amp, w == null ? null : w.speed,
                    gradient.isEmpty() ? null : gradient.peekLast(),
                    color.isEmpty()    ? null : color.peekLast(),
                    pulse.isEmpty()    ? null : pulse.peekLast(),
                    shake.isEmpty()    ? null : shake.peekLast(),
                    b == null ? null : b.amp, b == null ? null : b.speed,
                    wa == null ? null : wa.amp, wa == null ? null : wa.speed,
                    glitch.isEmpty()   ? null : glitch.peekLast()
            );
        }

        void openRainbow(Float speed) { rainbow.addLast(speed); }
        void closeRainbow() { if(!rainbow.isEmpty()) rainbow.removeLast(); }

        void openWiggle(Float amp, Float speed) { wiggle.addLast(new Wiggle(amp, speed)); }
        void closeWiggle() { if(!wiggle.isEmpty()) wiggle.removeLast(); }

        void openGradient(int[] cols) { gradient.addLast(cols); }
        void closeGradient() { if(!gradient.isEmpty()) gradient.removeLast(); }

        void openPulse(Float s) { pulse.addLast(s); }
        void closePulse() { if(!pulse.isEmpty()) pulse.removeLast(); }

        void openShake(Float a) { shake.addLast(a); }
        void closeShake() { if(!shake.isEmpty()) shake.removeLast(); }

        void openColor(Integer c) { color.addLast(c); }
        void closeColor() { if(!color.isEmpty()) color.removeLast(); }

        void openBounce(Float amp, Float speed) { bounce.addLast(new Bounce(amp, speed)); }
        void closeBounce() { if(!bounce.isEmpty()) bounce.removeLast(); }

        void openWave(Float amp, Float speed) { wave.addLast(new Wave(amp, speed)); }
        void closeWave() { if(!wave.isEmpty()) wave.removeLast(); }

        void openGlitch(Float intensity) { glitch.addLast(intensity); }
        void closeGlitch() { if(!glitch.isEmpty()) glitch.removeLast(); }
    }

    public static List<Span> parseSpans(String raw) {
        if (raw == null || raw.isEmpty()) {
            List<Span> only = new ArrayList<>();
            only.add(new Span("", false,null, false,null,null, null, null, null,null, null,null, null,null, null));
            return only;
        }

        Stacks st = new Stacks();
        List<Span> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        int i = 0;
        while (i < raw.length()) {
            char ch = raw.charAt(i);

            if (ch == '{') {
                int close = raw.indexOf('}', i);
                if (close > i) {
                    String inside = raw.substring(i + 1, close).trim();
                    boolean isClose = inside.startsWith("/");
                    String namePayload = isClose ? inside.substring(1).trim() : inside;
                    int colon = namePayload.indexOf(':');
                    String name = (colon >= 0 ? namePayload.substring(0, colon) : namePayload).trim().toLowerCase();
                    String payload = (colon >= 0 ? namePayload.substring(colon + 1) : "").trim();

                    if (isKnownName(name)) {
                        if (sb.length() > 0) {
                            out.add(st.toSpan(sb.toString()));
                            sb.setLength(0);
                        }
                        if (isClose) {
                            applyClose(st, name);      // stray closers are ignored by design
                        } else {
                            applyOpen(st, name, payload);
                        }
                        i = close + 1;
                        continue;
                    }
                }
            }

            if (ch == '&' && i + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(i + 1));
                if (code == 'r') {
                    if (sb.length() > 0) { out.add(st.toSpan(sb.toString())); sb.setLength(0); }
                    st.color.clear();
                    i += 2;
                    continue;
                }
                Integer mapped = mapLegacyColor(code);
                if (mapped != null) {
                    if (sb.length() > 0) { out.add(st.toSpan(sb.toString())); sb.setLength(0); }
                    st.openColor(mapped);
                    i += 2;
                    continue;
                }
            }

            sb.append(ch);
            i++;
        }

        if (sb.length() > 0) {
            out.add(st.toSpan(sb.toString()));
        }
        if (out.isEmpty()) {
            out.add(new Span("", false,null, false,null,null, null, null, null,null, null,null, null,null, null));
        }
        return out;
    }

    private static boolean isKnownName(String n) {
        return n.equals("rainbow") || n.startsWith("rainbow")
                || n.equals("wiggle")  || n.startsWith("wiggle")
                || n.equals("gradient")|| n.startsWith("gradient")
                || n.equals("pulse")   || n.startsWith("pulse")
                || n.equals("shake")   || n.startsWith("shake")
                || n.equals("color")   || n.startsWith("color")
                || n.equals("bounce")  || n.startsWith("bounce")
                || n.equals("wave")    || n.startsWith("wave")
                || n.equals("glitch")  || n.startsWith("glitch")
                || n.equals("clear")   || n.equals("reset");
    }

    private static void applyOpen(Stacks st, String name, String payload) {
        if (name.startsWith("rainbow")) {
            st.openRainbow(parseFloatSafe(payload));
            return;
        }
        if (name.startsWith("wiggle")) {
            st.openWiggle(readKeyedFloat(payload, "amp"), readKeyedFloat(payload, "speed"));
            return;
        }
        if (name.startsWith("gradient")) {
            st.openGradient(parseGradient(payload));
            return;
        }
        if (name.startsWith("pulse")) {
            st.openPulse(parseFloatSafe(payload));
            return;
        }
        if (name.startsWith("shake")) {
            st.openShake(parseFloatSafe(payload));
            return;
        }
        if (name.startsWith("color")) {
            st.openColor(parseHexSafe(payload));
            return;
        }
        if (name.startsWith("bounce")) {
            st.openBounce(readKeyedFloat(payload, "amp"), readKeyedFloat(payload, "speed"));
            return;
        }
        if (name.startsWith("wave")) {
            st.openWave(readKeyedFloat(payload, "amp"), readKeyedFloat(payload, "speed"));
            return;
        }
        if (name.startsWith("glitch")) {
            st.openGlitch(readKeyedFloat(payload, "intensity"));
            return;
        }
        if (name.equals("clear") || name.equals("reset")) {
            st.clearAll();
        }
    }

    private static void applyClose(Stacks st, String name) {
        if (name.equals("rainbow")) { st.closeRainbow(); return; }
        if (name.equals("wiggle"))  { st.closeWiggle();  return; }
        if (name.equals("gradient")){ st.closeGradient();return; }
        if (name.equals("pulse"))   { st.closePulse();   return; }
        if (name.equals("shake"))   { st.closeShake();   return; }
        if (name.equals("color"))   { st.closeColor();   return; }
        if (name.equals("bounce"))  { st.closeBounce();  return; }
        if (name.equals("wave"))    { st.closeWave();    return; }
        if (name.equals("glitch"))  { st.closeGlitch();  return; }
        if (name.equals("clear") || name.equals("reset")) {
            st.clearAll();
        }
    }

    private static Float parseFloatSafe(String s) {
        try { return s == null || s.isEmpty() ? null : Float.parseFloat(s.trim()); }
        catch (Exception ignored) { return null; }
    }

    private static Integer parseHexSafe(String s) {
        if (s == null) return null;
        String v = s.replace("#","").trim();
        try { return (int)Long.parseLong(v, 16) & 0xFFFFFF; }
        catch (Exception ignored) { return null; }
    }

    private static Float readKeyedFloat(String payload, String key) {
        if (payload == null) return null;
        for (String kv : payload.split(",")) {
            int eq = kv.indexOf('=');
            if (eq > 0 && key.equalsIgnoreCase(kv.substring(0, eq).trim())) {
                try { return Float.parseFloat(kv.substring(eq+1).trim()); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static int[] parseGradient(String payload) {
        if (payload == null || payload.isEmpty()) return null;
        String[] cols = payload.split(",");
        int[] out = new int[cols.length];
        int n = 0;
        for (String c : cols) {
            Integer v = parseHexSafe(c);
            if (v != null) out[n++] = v;
        }
        if (n == 0) return null;
        if (n == out.length) return out;
        int[] shrunk = new int[n];
        System.arraycopy(out, 0, shrunk, 0, n);
        return shrunk;
    }

    public static Parsed parse(String raw) {
        boolean rainbow = tokenPayload(raw, "rainbow") != null;
        boolean wiggle  = tokenPayload(raw, "wiggle")  != null;

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

        String s = raw.trim();
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
        while (true) {
            int i = s.indexOf("{/"); if (i < 0) break;
            int end = s.indexOf('}', i); if (end < 0) break;
            s = s.substring(0, i) + s.substring(end+1);
        }
        return s;
    }

    private static Integer readHexColor(String s) {
        String p = tokenPayload(s, "color");
        if (p == null || p.isEmpty()) return null;
        try { return (int)Long.parseLong(p.replace("#","").trim(), 16) & 0xFFFFFF; } catch (Exception ignore) { return null; }
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
            try { out[n++] = (int)Long.parseLong(c.replace("#","").trim(), 16) & 0xFFFFFF; } catch (Exception ignore) {}
        }
        return n == 0 ? null : (n == out.length ? out : java.util.Arrays.copyOf(out, n));
    }

    public static int resolveOrWhite(Integer rgb) { return rgb != null ? rgb : 0xFFFFFF; }

    public static int rainbowRgb(long nowMs, int index) {
        float t = (nowMs % 1000000L) / 1000.0f;
        float hue = wrap01(t * 0.18f + index * 0.12f);
        return hsbToRgb(hue, 1.0f, 1.0f);
    }

    public static int rainbowRgb(long nowMs, int index, float speed) {
        float t = (nowMs % 1_000_000L) / 1000.0f;
        float hue = wrap01(t * speed + index * 0.12f);
        return hsbToRgb(hue, 1f, 1f);
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

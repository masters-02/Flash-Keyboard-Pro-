/*
 * SinglishConverter.java
 * ──────────────────────────────────────────────────────────────────────
 * Flash Keyboard Pro — Sinhala Singlish (phonetic) input engine.
 *
 * Converts a buffer of Latin characters typed by the user into the
 * corresponding Sinhala Unicode characters using a longest-match
 * phonetic mapping table.
 *
 * How to integrate:
 *   1. Place this file in the same package as InputLogic.java.
 *   2. In InputLogic.handleNonFunctionalEvent() (or wherever a character
 *      is committed), call SinglishConverter.process(mBuffer) and commit
 *      the result instead of the raw character.  See the companion patch
 *      in InputLogic.java for the exact integration point.
 *
 * The mapping follows the widely-used "Singlish" (Sinhala Transliteration)
 * convention, where each English phoneme maps to a Sinhala akshara.
 *
 * Package: com.flashkeyboard.pro.inputmethod.latin.singlish
 * ──────────────────────────────────────────────────────────────────────
 */

package com.flashkeyboard.pro.inputmethod.latin.singlish;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stateless utility that converts a Latin phonetic string ("Singlish") to
 * Sinhala Unicode.  All methods are static; no instantiation needed.
 *
 * <p>Usage:
 * <pre>
 *   String sinhala = SinglishConverter.convert("ka");   // returns "ක"
 *   String sinhala = SinglishConverter.convert("kaa");  // returns "කා"
 *   String sinhala = SinglishConverter.convert("sri");  // returns "ශ්‍රී"
 * </pre>
 */
public final class SinglishConverter {

    // ── Sinhala Unicode constants ─────────────────────────────────────

    // Independent vowels
    private static final String A   = "අ";
    private static final String AA  = "ආ";
    private static final String AE  = "ඇ";
    private static final String AEE = "ඈ";
    private static final String I   = "ඉ";
    private static final String II  = "ඊ";
    private static final String U   = "උ";
    private static final String UU  = "ඌ";
    private static final String E   = "එ";
    private static final String EE  = "ඒ";
    private static final String AI  = "ඓ";
    private static final String O   = "ඔ";
    private static final String OO  = "ඕ";
    private static final String AU  = "ඖ";

    // Vowel signs (diacritics, appended to a consonant)
    private static final String SIGN_AA  = "ා";
    private static final String SIGN_AE  = "ැ";
    private static final String SIGN_AEE = "ෑ";
    private static final String SIGN_I   = "ි";
    private static final String SIGN_II  = "ී";
    private static final String SIGN_U   = "ු";
    private static final String SIGN_UU  = "ූ";
    private static final String SIGN_E   = "ෙ";
    private static final String SIGN_EE  = "ේ";
    private static final String SIGN_AI  = "ෛ";
    private static final String SIGN_O   = "ො";
    private static final String SIGN_OO  = "ෝ";
    private static final String SIGN_AU  = "ෞ";

    // Virama (hal kirima – suppresses the inherent vowel)
    private static final String VIRAMA = "්";

    // ── Mapping table (longest-match first) ──────────────────────────
    //
    // Rules:
    //  • A bare consonant key (e.g. "k") emits the consonant + inherent
    //    vowel "a" in Sinhala, so "k" → "ක".
    //  • Doubling the vowel lengthens it: "kaa" → "කා".
    //  • Consonant clusters use virama: "k_" → "ක්" (hal form).
    //
    // LinkedHashMap preserves insertion order, which is important so
    // that longer keys are matched before their shorter prefixes.
    //
    private static final Map<String, String> TABLE = new LinkedHashMap<>();

    static {
        // ── Consonant + vowel combos (most specific first) ────────────

        // k  ────────────────────────────────────────────────
        TABLE.put("kaa",  "කා");
        TABLE.put("kae",  "කැ");
        TABLE.put("kaee", "කෑ");
        TABLE.put("ki",   "කි");
        TABLE.put("kii",  "කී");
        TABLE.put("ku",   "කු");
        TABLE.put("kuu",  "කූ");
        TABLE.put("ke",   "කෙ");
        TABLE.put("kee",  "කේ");
        TABLE.put("ko",   "කො");
        TABLE.put("koo",  "කෝ");
        TABLE.put("kau",  "කෞ");
        TABLE.put("ka",   "ක");
        TABLE.put("k",    "ක");   // bare – inherent 'a' included

        // g  ────────────────────────────────────────────────
        TABLE.put("gaa",  "ගා");
        TABLE.put("gi",   "ගි");
        TABLE.put("gii",  "ගී");
        TABLE.put("gu",   "ගු");
        TABLE.put("guu",  "ගූ");
        TABLE.put("ge",   "ගෙ");
        TABLE.put("gee",  "ගේ");
        TABLE.put("go",   "ගො");
        TABLE.put("goo",  "ගෝ");
        TABLE.put("ga",   "ග");
        TABLE.put("g",    "ග");

        // ng / ṅ ────────────────────────────────────────────
        TABLE.put("nga",  "ඞ");
        TABLE.put("ng",   "ඞ");

        // ch / c ────────────────────────────────────────────
        TABLE.put("chaa", "චා");
        TABLE.put("chi",  "චි");
        TABLE.put("chii", "චී");
        TABLE.put("chu",  "චු");
        TABLE.put("chuu", "චූ");
        TABLE.put("che",  "චෙ");
        TABLE.put("chee", "චේ");
        TABLE.put("cho",  "චො");
        TABLE.put("choo", "චෝ");
        TABLE.put("cha",  "ච");
        TABLE.put("ch",   "ච");

        // j  ────────────────────────────────────────────────
        TABLE.put("jaa",  "ජා");
        TABLE.put("ji",   "ජි");
        TABLE.put("jii",  "ජී");
        TABLE.put("ju",   "ජු");
        TABLE.put("juu",  "ජූ");
        TABLE.put("je",   "ජෙ");
        TABLE.put("jee",  "ජේ");
        TABLE.put("jo",   "ජො");
        TABLE.put("joo",  "ජෝ");
        TABLE.put("ja",   "ජ");
        TABLE.put("j",    "ජ");

        // t / T (ට – retroflex) ────────────────────────────
        TABLE.put("Taa",  "ටා");
        TABLE.put("Ti",   "ටි");
        TABLE.put("Tu",   "ටු");
        TABLE.put("Te",   "ටෙ");
        TABLE.put("To",   "ටො");
        TABLE.put("Ta",   "ට");
        TABLE.put("T",    "ට");

        // D (ඩ – retroflex) ────────────────────────────────
        TABLE.put("Daa",  "ඩා");
        TABLE.put("Di",   "ඩි");
        TABLE.put("Du",   "ඩු");
        TABLE.put("De",   "ඩෙ");
        TABLE.put("Do",   "ඩො");
        TABLE.put("Da",   "ඩ");
        TABLE.put("D",    "ඩ");

        // n / N ─────────────────────────────────────────────
        TABLE.put("naa",  "නා");
        TABLE.put("ni",   "නි");
        TABLE.put("nii",  "නී");
        TABLE.put("nu",   "නු");
        TABLE.put("nuu",  "නූ");
        TABLE.put("ne",   "නෙ");
        TABLE.put("nee",  "නේ");
        TABLE.put("no",   "නො");
        TABLE.put("noo",  "නෝ");
        TABLE.put("na",   "න");
        TABLE.put("n",    "න");

        // p  ────────────────────────────────────────────────
        TABLE.put("paa",  "පා");
        TABLE.put("pi",   "පි");
        TABLE.put("pii",  "පී");
        TABLE.put("pu",   "පු");
        TABLE.put("puu",  "පූ");
        TABLE.put("pe",   "පෙ");
        TABLE.put("pee",  "පේ");
        TABLE.put("po",   "පො");
        TABLE.put("poo",  "පෝ");
        TABLE.put("pau",  "පෞ");
        TABLE.put("pa",   "ප");
        TABLE.put("p",    "ප");

        // b  ────────────────────────────────────────────────
        TABLE.put("baa",  "බා");
        TABLE.put("bi",   "බි");
        TABLE.put("bii",  "බී");
        TABLE.put("bu",   "බු");
        TABLE.put("buu",  "බූ");
        TABLE.put("be",   "බෙ");
        TABLE.put("bee",  "බේ");
        TABLE.put("bo",   "බො");
        TABLE.put("boo",  "බෝ");
        TABLE.put("ba",   "බ");
        TABLE.put("b",    "බ");

        // m  ────────────────────────────────────────────────
        TABLE.put("maa",  "මා");
        TABLE.put("mi",   "මි");
        TABLE.put("mii",  "මී");
        TABLE.put("mu",   "මු");
        TABLE.put("muu",  "මූ");
        TABLE.put("me",   "මෙ");
        TABLE.put("mee",  "මේ");
        TABLE.put("mo",   "මො");
        TABLE.put("moo",  "මෝ");
        TABLE.put("ma",   "ම");
        TABLE.put("m",    "ම");

        // y  ────────────────────────────────────────────────
        TABLE.put("yaa",  "යා");
        TABLE.put("yi",   "යි");
        TABLE.put("yu",   "යු");
        TABLE.put("ye",   "යෙ");
        TABLE.put("yo",   "යො");
        TABLE.put("ya",   "ය");
        TABLE.put("y",    "ය");

        // r  ────────────────────────────────────────────────
        TABLE.put("raa",  "රා");
        TABLE.put("ri",   "රි");
        TABLE.put("ruu",  "රූ");
        TABLE.put("re",   "රෙ");
        TABLE.put("ro",   "රො");
        TABLE.put("ra",   "ර");
        TABLE.put("r",    "ර");

        // l  ────────────────────────────────────────────────
        TABLE.put("laa",  "ලා");
        TABLE.put("li",   "ලි");
        TABLE.put("lu",   "ලු");
        TABLE.put("le",   "ලෙ");
        TABLE.put("lo",   "ලො");
        TABLE.put("la",   "ල");
        TABLE.put("l",    "ල");

        // v / w ─────────────────────────────────────────────
        TABLE.put("vaa",  "වා");
        TABLE.put("vi",   "වි");
        TABLE.put("vu",   "වු");
        TABLE.put("ve",   "වෙ");
        TABLE.put("vo",   "වො");
        TABLE.put("va",   "ව");
        TABLE.put("v",    "ව");
        TABLE.put("waa",  "වා");
        TABLE.put("wi",   "වි");
        TABLE.put("wu",   "වු");
        TABLE.put("we",   "වෙ");
        TABLE.put("wo",   "වො");
        TABLE.put("wa",   "ව");
        TABLE.put("w",    "ව");

        // sh / S ────────────────────────────────────────────
        TABLE.put("shaa", "ශා");
        TABLE.put("shi",  "ශි");
        TABLE.put("shu",  "ශු");
        TABLE.put("she",  "ශෙ");
        TABLE.put("sho",  "ශො");
        TABLE.put("sha",  "ශ");
        TABLE.put("sh",   "ශ");

        // s  ────────────────────────────────────────────────
        TABLE.put("saa",  "සා");
        TABLE.put("si",   "සි");
        TABLE.put("sii",  "සී");
        TABLE.put("su",   "සු");
        TABLE.put("suu",  "සූ");
        TABLE.put("se",   "සෙ");
        TABLE.put("see",  "සේ");
        TABLE.put("so",   "සො");
        TABLE.put("soo",  "සෝ");
        TABLE.put("sa",   "ස");
        TABLE.put("s",    "ස");

        // h  ────────────────────────────────────────────────
        TABLE.put("haa",  "හා");
        TABLE.put("hi",   "හි");
        TABLE.put("hii",  "හී");
        TABLE.put("hu",   "හු");
        TABLE.put("huu",  "හූ");
        TABLE.put("he",   "හෙ");
        TABLE.put("hee",  "හේ");
        TABLE.put("ho",   "හො");
        TABLE.put("hoo",  "හෝ");
        TABLE.put("ha",   "හ");
        TABLE.put("h",    "හ");

        // f  ────────────────────────────────────────────────
        TABLE.put("faa",  "ෆා");
        TABLE.put("fi",   "ෆි");
        TABLE.put("fu",   "ෆු");
        TABLE.put("fe",   "ෆෙ");
        TABLE.put("fo",   "ෆො");
        TABLE.put("fa",   "ෆ");
        TABLE.put("f",    "ෆ");

        // ── Independent vowels ────────────────────────────────────────
        TABLE.put("aa",   AA);
        TABLE.put("ae",   AE);
        TABLE.put("aee",  AEE);
        TABLE.put("ai",   AI);
        TABLE.put("au",   AU);
        TABLE.put("ee",   EE);
        TABLE.put("ii",   II);
        TABLE.put("oo",   OO);
        TABLE.put("uu",   UU);
        TABLE.put("a",    A);
        TABLE.put("e",    E);
        TABLE.put("i",    I);
        TABLE.put("o",    O);
        TABLE.put("u",    U);

        // ── Common two-consonant clusters (using virama) ──────────────
        TABLE.put("ndha", "න්ද");
        TABLE.put("mba",  "ම්බ");
        TABLE.put("stu",  "ස්තු");
        TABLE.put("str",  "ස්ත්‍ර");
        TABLE.put("sth",  "ස්ථ");
        TABLE.put("ksh",  "ක්ෂ");
        TABLE.put("gny",  "ඥ");
        TABLE.put("sri",  "ශ්‍රී");
        TABLE.put("sra",  "ශ්‍ර");

        // ── Anusvara / Visarga ────────────────────────────────────────
        TABLE.put("M",    "ං");   // anusvara (nasalization dot)
        TABLE.put("H",    "ඃ");   // visarga
    }

    // Private constructor – static-only utility.
    private SinglishConverter() {}

    /**
     * Convert a phonetic Latin string to Sinhala Unicode using a
     * greedy longest-match algorithm.
     *
     * @param input Latin phonetic string (e.g. {@code "sinhala"})
     * @return Sinhala Unicode string, or the original input if no
     *         mapping is found.
     */
    public static String convert(final String input) {
        if (input == null || input.isEmpty()) return input;

        final StringBuilder result = new StringBuilder();
        int pos = 0;
        final int len = input.length();

        while (pos < len) {
            String matched = null;
            int matchLen  = 0;

            // Try to match the longest possible sequence starting at pos.
            // TABLE keys range from 1 to 4 characters.
            final int maxLook = Math.min(4, len - pos);
            for (int look = maxLook; look >= 1; look--) {
                final String candidate = input.substring(pos, pos + look);
                final String sinhala   = TABLE.get(candidate);
                if (sinhala != null) {
                    matched  = sinhala;
                    matchLen = look;
                    break;
                }
            }

            if (matched != null) {
                result.append(matched);
                pos += matchLen;
            } else {
                // No mapping – pass the character through unchanged.
                result.append(input.charAt(pos));
                pos++;
            }
        }
        return result.toString();
    }

    /**
     * Returns {@code true} if the given string is a valid (or partial)
     * prefix of any key in the mapping table.  Useful for deciding whether
     * to keep composing or to flush the buffer.
     *
     * @param prefix the current composition buffer
     */
    public static boolean isValidPrefix(final String prefix) {
        if (prefix == null || prefix.isEmpty()) return false;
        for (final String key : TABLE.keySet()) {
            if (key.startsWith(prefix)) return true;
        }
        return false;
    }
}

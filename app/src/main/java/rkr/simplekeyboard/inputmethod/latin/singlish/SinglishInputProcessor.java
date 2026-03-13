/*
 * SinglishInputProcessor.java
 * ──────────────────────────────────────────────────────────────────────
 * Flash Keyboard Pro — Singlish composition state machine.
 *
 * This class manages the in-progress Latin character buffer and decides
 * when to flush (commit) Sinhala text to the editor.
 *
 * Integration pattern (add to InputLogic):
 *
 *   // Field
 *   private final SinglishInputProcessor mSinglishProcessor =
 *       new SinglishInputProcessor();
 *
 *   // In handleNonFunctionalEvent() / handleSeparatorEvent(), replace
 *   // the raw commitText call with:
 *   if (isSinglishSubtype()) {
 *       final String commit = mSinglishProcessor.onChar((char) event.mCodePoint);
 *       if (commit != null) mConnection.commitText(commit, 1);
 *   } else {
 *       mConnection.commitText(StringUtils.newSingleCodePointString(event.mCodePoint), 1);
 *   }
 *
 * Package: com.flashkeyboard.pro.inputmethod.latin.singlish
 * ──────────────────────────────────────────────────────────────────────
 */

package rkr.simplekeyboard.inputmethod.latin.singlish;

/**
 * Stateful composition buffer for Singlish (phonetic Sinhala) input.
 *
 * <p>The processor accumulates Latin characters typed by the user.  After
 * each character it checks whether the accumulated string still matches a
 * valid prefix in the mapping table.  If it does, the buffer is held and
 * nothing is committed yet (the caller should set the composing text to the
 * raw buffer so the user can see what they have typed).  As soon as a
 * character breaks any possible match, the buffer is flushed: the longest
 * matched prefix is converted to Sinhala, any unmatched remainder plus the
 * new character start a fresh buffer.
 */
public final class SinglishInputProcessor {

    /** Maximum look-ahead (longest key in the mapping table). */
    private static final int MAX_BUFFER = 6;

    private final StringBuilder mBuffer = new StringBuilder(MAX_BUFFER);

    /** Call this when the user's subtype changes away from Singlish. */
    public void reset() {
        mBuffer.setLength(0);
    }

    /**
     * Process one character from the user.
     *
     * @param c the Latin character just typed
     * @return  a string to commit to the editor, or {@code null} if the
     *          character was absorbed into the composing buffer.  When
     *          non-null the caller should commit this string and set the
     *          composing region to the new (possibly empty) buffer content.
     */
    public String onChar(final char c) {
        mBuffer.append(c);
        final String candidate = mBuffer.toString();

        if (SinglishConverter.isValidPrefix(candidate)) {
            // Still composing – don't commit yet.
            return null;
        }

        // The current buffer is not a valid prefix any more.
        // Walk back to find the longest committable sub-string.
        String toCommit = "";
        String remainder = candidate;

        for (int splitAt = candidate.length() - 1; splitAt >= 1; splitAt--) {
            final String head = candidate.substring(0, splitAt);
            final String tail = candidate.substring(splitAt);
            final String converted = SinglishConverter.convert(head);
            // If convert() changed the string, we have a real match.
            if (!converted.equals(head)) {
                toCommit  = converted;
                remainder = tail;
                break;
            }
        }

        // Reset buffer to the unmatched remainder.
        mBuffer.setLength(0);
        mBuffer.append(remainder);

        // If the remainder is itself a valid prefix keep it composing;
        // otherwise flush it too.
        if (!SinglishConverter.isValidPrefix(remainder)) {
            final String flushed = SinglishConverter.convert(remainder);
            mBuffer.setLength(0);
            return toCommit + flushed;
        }

        return toCommit.isEmpty() ? null : toCommit;
    }

    /**
     * Called when the user presses a non-letter key (space, punctuation,
     * enter, etc.).  Flushes the current buffer and returns its Sinhala
     * equivalent plus the separator character.
     *
     * @param separator the separator code point (e.g. {@code ' '})
     * @return text to commit (converted buffer + separator)
     */
    public String onSeparator(final int separator) {
        final String buffered = mBuffer.toString();
        mBuffer.setLength(0);
        final String converted = SinglishConverter.convert(buffered);
        return converted + new String(Character.toChars(separator));
    }

    /**
     * Called when the user presses Backspace.  Removes the last character
     * from the composing buffer.
     *
     * @return {@code true} if the buffer was non-empty and a character was
     *         removed (the caller should update the composing region).
     *         {@code false} if the buffer was already empty (the caller
     *         should perform a normal delete).
     */
    public boolean onBackspace() {
        if (mBuffer.length() > 0) {
            mBuffer.deleteCharAt(mBuffer.length() - 1);
            return true;
        }
        return false;
    }

    /** Returns the current raw Latin composing buffer (for display). */
    public String getBuffer() {
        return mBuffer.toString();
    }

    /** Returns {@code true} when there are uncommitted characters. */
    public boolean hasBuffer() {
        return mBuffer.length() > 0;
    }

    /**
     * Flush the entire buffer immediately (e.g. on cursor movement).
     *
     * @return the converted Sinhala string, or an empty string if the
     *         buffer was already empty.
     */
    public String flush() {
        if (mBuffer.length() == 0) return "";
        final String converted = SinglishConverter.convert(mBuffer.toString());
        mBuffer.setLength(0);
        return converted;
    }
}

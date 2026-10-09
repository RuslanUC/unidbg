package com.github.unidbg.file;

import com.github.unidbg.Emulator;

/**
 * Optional resolver capability for {@code stat}-family calls. Unlike
 * {@link IOResolver}, which models {@code open} (requiring file access
 * rights), stat only requires search permission on parent directories.
 * Implementors must therefore check parent traversal rights, not file
 * read rights.
 *
 * <p>Return {@code null} for paths this resolver does not handle so the
 * caller falls through to the regular resolution. A failed result is
 * terminal. A successful result carries an {@code O_RDONLY}-opened IO used
 * solely for {@code fstat}; the caller closes it.
 */
public interface StatResolver<T extends NewFileIO> {

    FileResult<T> resolveStat(Emulator<T> emulator, String pathname);

}

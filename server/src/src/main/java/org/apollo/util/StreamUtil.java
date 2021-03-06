package org.apollo.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A class which contains {@link InputStream}- and {@link OutputStream}-related
 * utility methods.
 *
 * @author Graham
 */
public final class StreamUtil {

	/**
	 * Writes a string to the specified output stream.
	 *
	 * @param os The output stream.
	 * @param str The string.
	 * @throws IOException if an I/O error occurs.
	 */
	public static void writeString(OutputStream os, String str) throws IOException {
		for (char c : str.toCharArray()) {
			os.write(c);
		}
		os.write('\0');
	}

	/**
	 * Reads a string from the specified input stream.
	 *
	 * @param is The input stream.
	 * @return The string.
	 * @throws IOException if an I/O error occurs.
	 */
	public static String readString(InputStream is) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		for (;;) {
			int read = is.read() & 0xFF;
			if (read == -1 || read == '\0') {
				break;
			}
			os.write(read);
		}

		return new String(os.toByteArray());
	}

	/**
	 * Suppresses the default-public constructor preventing this class from
	 * being instantiated by other classes.
	 *
	 * @throws InstantiationError If this class is instantiated within itself.
	 */
	private StreamUtil() {
		throw new InstantiationError("static-utility classes may not be instantiated.");
	}

}
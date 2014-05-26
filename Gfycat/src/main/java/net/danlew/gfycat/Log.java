package net.danlew.gfycat;

/**
 * Custom implementation of Log that adds extra functionality.  It functions
 * as a complete replacement for Log.
 * 
 * Its main advantage is in allowing you to configure defaults, thus
 * simplifying logging code.  It lets you configure a default tag,
 * which is used when the tag is not specified.  It also configures
 * a logging level, which allows you to enable/disable logging
 * statements at a particular level (or enable/disable them entirely).
 * 
 * It also has a convenience method for dumping long messages to the logs.
 * Since the length of debug 
 * 
 * In addition, it provides backwards compatibility for methods added in 
 * later versions of Android (like Log.wtf()).  This support goes back to
 * Android 1.5.
 * 
 * There are two warnings about using this class.  The first is that it's not
 * a full replacement of Log - there are some methods in android.util.Log that
 * take a tag and a Throwable (namely, w() and wtf()) and those are not present
 * here (due to method signature collision).  The second warning is that this
 * class is potentially slower than using android.util.Log due to added
 * overhead with method calls (and the need to pre-format text).
 */
public class Log {

	public static final int VERBOSE = android.util.Log.VERBOSE;

	public static final int DEBUG = android.util.Log.DEBUG;

	public static final int INFO = android.util.Log.INFO;

	public static final int WARN = android.util.Log.WARN;

	public static final int ERROR = android.util.Log.ERROR;

	public static final int ASSERT = android.util.Log.ASSERT;

	public static final int NONE = Integer.MAX_VALUE;

	/**
	 * The default tag used for logging (if not yet configured).
	 */
	public static final String DEFAULT_TAG = "default";

	private static String sTag = DEFAULT_TAG;

	private static int sLevel = VERBOSE;

	/**
	 * Configure the defaults for logging.
	 * 
	 * @param tag the default logging tag
	 * @param level the minimum level at which statements are logged 
	 */
	public static void configure(String tag, int level) {
		sTag = tag;
		sLevel = level;
	}

	/**
	 * Configure the defaults for logging.  This method is a simpler
	 * version of the other configure() method, with less specificity
	 * needed for the level.
	 * 
	 * @param tag the default logging tag
	 * @param enabled whether logging should be enabled or not
	 */
	public static void configure(String tag, boolean enabled) {
		sTag = tag;
		sLevel = enabled ? VERBOSE : NONE;
	}

	/**
	 * @return the configured default logging tag
	 */
	public static String getTag() {
		return sTag;
	}

	/**
	 * @return the configured default logging level
	 */
	public static int getLevel() {
		return sLevel;
	}

	//////////////////////////////////////////////////////////////////////////
	// Logging

	public static int v(String msg) {
		return log(VERBOSE, sTag, msg);
	}

	public static int v(String msg, Throwable tr) {
		return log(VERBOSE, sTag, msg, tr);
	}

	public static int v(String tag, String msg) {
		return log(VERBOSE, tag, msg);
	}

	public static int v(String tag, String msg, Throwable tr) {
		return log(VERBOSE, tag, msg, tr);
	}

	public static int d(String msg) {
		return log(DEBUG, sTag, msg);
	}

	public static int d(String msg, Throwable tr) {
		return log(DEBUG, sTag, msg, tr);
	}

	public static int d(String tag, String msg) {
		return log(DEBUG, tag, msg);
	}

	public static int d(String tag, String msg, Throwable tr) {
		return log(DEBUG, tag, msg, tr);
	}

	public static int i(String msg) {
		return log(INFO, sTag, msg);
	}

	public static int i(String msg, Throwable tr) {
		return log(INFO, sTag, msg, tr);
	}

	public static int i(String tag, String msg) {
		return log(INFO, tag, msg);
	}

	public static int i(String tag, String msg, Throwable tr) {
		return log(INFO, tag, msg, tr);
	}

	public static int w(String msg) {
		return log(WARN, sTag, msg);
	}

	public static int w(String msg, Throwable tr) {
		return log(WARN, sTag, msg, tr);
	}

	public static int w(String tag, String msg) {
		return log(WARN, tag, msg);
	}

	public static int w(String tag, String msg, Throwable tr) {
		return log(WARN, tag, msg, tr);
	}

	public static int e(String msg) {
		return log(ERROR, sTag, msg);
	}

	public static int e(String msg, Throwable tr) {
		return log(ERROR, sTag, msg, tr);
	}

	public static int e(String tag, String msg) {
		return log(ERROR, tag, msg);
	}

	public static int e(String tag, String msg, Throwable tr) {
		return log(ERROR, tag, msg, tr);
	}

	public static int wtf(String msg) {
		return log(ASSERT, sTag, msg);
	}

	public static int wtf(String msg, Throwable tr) {
		return log(ASSERT, sTag, msg, tr);
	}

	public static int wtf(String tag, String msg) {
		return log(ASSERT, tag, msg);
	}

	public static int wtf(String tag, String msg, Throwable tr) {
		return log(ASSERT, tag, msg, tr);
	}

	public static int log(int level, String msg) {
		return log(level, sTag, msg);
	}

	public static int log(int level, String tag, String msg) {
		if (sLevel > level) {
			return -1;
		}

		switch (level) {
		case VERBOSE:
			return android.util.Log.v(tag, msg);
		case DEBUG:
			return android.util.Log.d(tag, msg);
		case INFO:
			return android.util.Log.i(tag, msg);
		case WARN:
			return android.util.Log.w(tag, msg);
		case ERROR:
			return android.util.Log.e(tag, msg);
		case ASSERT:
			if (mWtfAvailable) {
				return WtfWrapper.wtf(tag, msg);
			}
			else {
				return android.util.Log.println(android.util.Log.ASSERT, tag, msg);
			}
		}

		// Provided invalid level
		return -1;
	}

	public static int log(int level, String msg, Throwable tr) {
		return log(level, sTag, msg, tr);
	}

	public static int log(int level, String tag, String msg, Throwable tr) {
		if (sLevel > level) {
			return -1;
		}

		switch (level) {
		case VERBOSE:
			return android.util.Log.v(tag, msg, tr);
		case DEBUG:
			return android.util.Log.d(tag, msg, tr);
		case INFO:
			return android.util.Log.i(tag, msg, tr);
		case WARN:
			return android.util.Log.w(tag, msg, tr);
		case ERROR:
			return android.util.Log.e(tag, msg, tr);
		case ASSERT:
			if (mWtfAvailable) {
				return WtfWrapper.wtf(tag, msg, tr);
			}
			else {
				return android.util.Log.println(android.util.Log.ASSERT, tag,
						msg + '\n' + android.util.Log.getStackTraceString(tr));
			}
		}

		// Provided invalid level
		return -1;
	}

	//////////////////////////////////////////////////////////////////////////
	// Dumps (logging long strings)

	// The maximum characters to dump on each line.  The official max is 4076,
	// however that also counts other portions of the dumped text (like tag
	// and timestamp), so this max works out pretty well.
	private static int DUMP_MAX = 4000;

	public static int dump(String longMsg) {
		return dump(sTag, longMsg, sLevel);
	}

	public static int dump(String longMsg, int level) {
		return dump(sTag, longMsg, level);
	}

	public static int dump(String tag, String longMsg) {
		return dump(tag, longMsg, sLevel);
	}

	public static int dump(String tag, String longMsg, int level) {
		if (level > sLevel) {
			return -1;
		}

		int bytes = 0;
		int start = 0;
		int len = longMsg.length();

		while (start < len) {
			int end = start + DUMP_MAX;
			if (end > len) {
				end = len;
			}
			String substr = longMsg.substring(start, end);

			switch (level) {
			case ASSERT:
				if (mWtfAvailable) {
					bytes += WtfWrapper.wtf(tag, substr);
				}
				else {
					bytes += android.util.Log.println(android.util.Log.ASSERT, tag, substr);
				}
				break;
			case ERROR:
				bytes += android.util.Log.e(tag, substr);
				break;
			case WARN:
				bytes += android.util.Log.w(tag, substr);
				break;
			case INFO:
				bytes += android.util.Log.i(tag, substr);
				break;
			case DEBUG:
				bytes += android.util.Log.d(tag, substr);
				break;
			case VERBOSE:
			default:
				bytes += android.util.Log.v(tag, substr);
				break;
			}

			start += DUMP_MAX;
		}

		return bytes;
	}

	//////////////////////////////////////////////////////////////////////////
	// Other methods from Log (wrapped here for convenience)

	public static String getStackTraceString(Throwable tr) {
		return android.util.Log.getStackTraceString(tr);
	}

	public static boolean isLoggable(String tag, int level) {
		return android.util.Log.isLoggable(tag, level);
	}

	public static int println(int priority, String tag, String msg) {
		return android.util.Log.println(priority, tag, msg);
	}

	//////////////////////////////////////////////////////////////////////////
	// Wrapper for wtf()

	private static boolean mWtfAvailable;

	static {
		try {
			WtfWrapper.checkAvailable();
			mWtfAvailable = true;
		}
		catch (Throwable t) {
			mWtfAvailable = false;
		}
	}

	private static class WtfWrapper {
		static {
			try {
				Log.class.getMethod("wtf", String.class, String.class);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		public static void checkAvailable() { }

		public static int wtf(String tag, String msg) {
			return android.util.Log.wtf(tag, msg);
		}

		public static int wtf(String tag, String msg, Throwable tr) {
			return android.util.Log.wtf(tag, msg, tr);
		}
	}
}

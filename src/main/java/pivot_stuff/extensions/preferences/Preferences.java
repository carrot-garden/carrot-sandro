/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pivot_stuff.extensions.preferences;

import java.io.IOException;
import java.util.MissingResourceException;

import org.apache.pivot.collections.Dictionary;
import org.apache.pivot.collections.HashMap;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Map;
import org.apache.pivot.json.JSONSerializer;
import org.apache.pivot.serialization.SerializationException;
import org.apache.pivot.serialization.Serializer;

/**
 * This is the base class for Preferences, organized as a Tree of Nodes (where
 * each Node is a Dictionary<String key, Object value>), usually in JSON format.
 * <p>
 * Any key used (a String) must be a valid JSON key (for example not null, not
 * empty, and a valid Java variable name), or the serializer will throw a
 * SerializationException during write/read operations. Values must be one of
 * the types handled by the serializer, too.
 * </p>
 * <p>
 * Note that these Preferences are used to store groups of settings, in a fixed
 * (Pivot) directory under the user profile. All Preferences for an Application
 * (name chosen by the user, for example a class name, or a title) are located
 * inside a dedicated sub-directory. Inside this, any Preferences instance is
 * stored in a physical file (1 per context).
 * </p>
 * <p>
 * Using this class from a restricted environment (Applet or Web Start, or
 * Application with a SecurityManager) read/write permissions are needed. But
 * this is handled in dedicated subclasses.
 * </p>
 * 
 * TODO: - verify if migrate this to the Task-based approach, like in
 * Preferences_temp (under _old) ...
 * 
 * @see java.util.prefs.Preferences
 * @see pivot.util.Resources
 */
public abstract class Preferences implements Dictionary<String, Object> {

	protected static final String CLASS_NAME = Preferences.class.getName();
	protected static final String PIVOT_PREFS_DIR = ".pivot"; // no-i18n
	protected static final String PIVOT_PREFS_DEFAULT_FILE = "defaultPreferences"; // no-i18n
	protected static final String PIVOT_PREFS_DEFAULT_EXT = ".json"; // no-i18n

	private boolean initialized = false;

	private String applicationName;
	private String contextName;

	protected String rootPath;
	protected String basePath;
	protected String baseName;
	private Map<String, Object> prefsMap;

	private Serializer<Object> serializer = null;

	/** Default Constructor */
	public Preferences() throws IOException, SerializationException {
		this(null);
	}

	/**
	 * Full constructor for a Preferences instance, setting non-variant
	 * attributes.
	 * 
	 * @param applicationName
	 *            the base name of this preferences set, for example a fully
	 *            qualified class name, or the name of the application for
	 *            grouping more preferences contexts, or if null the current
	 *            class name will be used
	 * @throws IOException
	 *             if there is a problem when reading the resource
	 * @throws SerializationException
	 *             if there is a problem deserializing the resource from its
	 *             JSON format
	 * @throws NullPointerException
	 *             if baseName is null
	 * @throws MissingResourceException
	 *             if no preferences for the specified base name can be found
	 */
	public Preferences(String applicationName) throws IOException,
			SerializationException {

		setSerializer(null);

		setApplicationName(applicationName);
		setRootPath();
		setBasePath(basePath);
		setBaseName(baseName);

		initialized = init();

	}

	/**
	 * Construction-only initialization.
	 * <p>
	 * Subclasses should have a custom (private like this) method, and put some
	 * privileged actions inside (like test for write on file system, or
	 * initialize services required), so after this, the isInitialized() method
	 * tells if all is right for the class to work (if not could be that not
	 * enough permissions are granted, or in not the right execution
	 * environment, etc).
	 * </p>
	 * 
	 * @return true if successful, otherwise false
	 */
	private boolean init() {
		clear();

		// additional (init-only) operations follows ...
		;

		return true;
	}

	/**
	 * Reset some variables
	 */
	public void clear() {
		// prefsMap = null;
		// keep this instead, to avoid npe on write operations ...
		prefsMap = new HashMap<String, Object>();

		setContextName(null);
	}

	/**
	 * Tells is the class has been initialized, or better, if has been
	 * successfully initialized.
	 * 
	 * @return true or false
	 */
	public final boolean isInitialized() {
		return initialized;
	}

	protected void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	protected final Serializer<Object> getSerializer() {
		return serializer;
	}

	public final String getApplicationName() {
		return applicationName;
	}

	public final String getContextName() {
		return contextName;
	}

	protected final void setSerializer(Serializer<Object> serializer) {
		if (serializer == null)
			serializer = new JSONSerializer();

		this.serializer = serializer;
	}

	protected final Map<String, Object> getPrefsMap() {
		return prefsMap;
	}

	protected final void setPrefsMap(Map<String, Object> prefsMap) {
		this.prefsMap = prefsMap;
	}

	protected final void setApplicationName(String applicationName) {
		if (applicationName == null || applicationName.length() < 1) {
			// throw new IllegalArgumentException("applicationName is null");
			applicationName = CLASS_NAME; // verify if keep this, or the
											// previous line ...
		}

		this.applicationName = applicationName;
	}

	public final void setContextName(String contextName) {
		if (contextName == null || contextName.length() < 1)
			contextName = PIVOT_PREFS_DEFAULT_FILE;

		this.contextName = contextName;
	}

	protected abstract void setRootPath();

	protected abstract void setBasePath(String basePath);

	protected abstract void setBaseName(String baseName);

	public final String getRootPath() {
		return rootPath;
	}

	public final String getBasePath() {
		return basePath;
	}

	public final String getBaseName() {
		return baseName;
	}

	public final boolean isEmpty() {
		return prefsMap.isEmpty();
	}

	public final boolean containsKey(String key) {
		return prefsMap.containsKey(key);
	}

	public final Object get(String key) {
		checkValidKey(key);

		return prefsMap.get(key);
	}

	public final Object put(String key, Object value) {
		checkValidKey(key);

		return prefsMap.put(key, value);
	}

	public final Object remove(String key) {
		checkValidKey(key);

		return prefsMap.remove(key);
	}

	public final Object exist(String key) {
		throw new UnsupportedOperationException("TODO");
		// checkValidKey(key);

		// return prefsMap.containsKey(key);
	}

	public final Object put(String key, Object value, Object defaultValue) {
		if (value == null)
			value = defaultValue;

		return prefsMap.put(key, value);
	}

	public final String getPreferencesPath() {
		return (rootPath + basePath + baseName + getPathSeparator());
	}

	public final String getContextResourceName() {
		return (getPreferencesPath() + getContextName() + PIVOT_PREFS_DEFAULT_EXT);
	}

	public static final boolean isValidKey(final String key) {
		boolean valid = false;

		if (key != null && key.length() > 0)
			valid = true;

		return valid;
	}

	public static final boolean checkValidKey(final String key) {
		if (!isValidKey(key))
			throw new IllegalArgumentException("not a valid key \"" + key
					+ "\"");
		else
			return true;
	}

	protected abstract String getPathSeparator();

	protected abstract boolean createPreferencesPath();

	protected abstract boolean deletePreferencesPath();

	protected abstract void writePreferences() throws IOException,
			SerializationException;

	protected abstract Map<String, Object> readPreferences()
			throws IOException, SerializationException;

	protected abstract boolean existPreferences() throws IOException,
			SerializationException;

	protected abstract boolean deletePreferences() throws IOException,
			SerializationException;

	protected abstract String dumpPreferences() throws IOException,
			SerializationException;

	public abstract List<String> listPreferencesContexts() throws IOException,
			SerializationException;

	public final boolean savePreferences(String contextName)
			throws IOException, SerializationException {
		if (contextName != null)
			setContextName(contextName);

		writePreferences();

		return true;
	}

	public final boolean loadPreferences(String contextName)
			throws IOException, SerializationException {
		if (contextName != null)
			setContextName(contextName);

		prefsMap = readPreferences();

		return true;
	}

	public final boolean existPreferences(String contextName)
			throws IOException, SerializationException {
		boolean success = false;

		if (contextName != null)
			setContextName(contextName);

		success = existPreferences();

		return success;
	}

	public final boolean deletePreferences(String contextName)
			throws IOException, SerializationException {
		boolean success = false;

		if (contextName != null)
			setContextName(contextName);

		success = deletePreferences();

		return success;
	}

	public final String dumpPreferences(String contextName) throws IOException,
			SerializationException {
		if (contextName != null)
			setContextName(contextName);

		String dump = null;

		dump = dumpPreferences();

		return dump;
	}

	@Override
	public String toString() {
		return CLASS_NAME + " : { " + "applicationName: \""
				+ getApplicationName() + "\", " + "contextName: \""
				+ getContextName() + "\", " + "serializer: " + getSerializer()
				+ ", " + "preferences: " + getPrefsMap() + " }";
	}

}

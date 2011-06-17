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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.MissingResourceException;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.Dictionary;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Map;
import org.apache.pivot.serialization.SerializationException;

/**
 * This class reads and writes a Tree of Preferences in a JSON format, where
 * each Node is a Dictionary<String, Object>. <br/>
 * This implementation reads and writes data from the (local) FileSystem. <br/>
 * <p>
 * When using this class from a restricted environment (Applet or Web Start, or
 * Application with a SecurityManager), read/write permissions for disk
 * resources are needed.
 * </p>
 * 
 * @see java.util.prefs.Preferences
 * @see pivot.util.Resources
 */
public class PreferencesFileSystem extends Preferences implements
		Dictionary<String, Object> {

	protected static final String CLASS_NAME = PreferencesFileSystem.class
			.getName();

	/** Default Constructor */
	public PreferencesFileSystem() throws IOException, SerializationException {
		super();

		setInitialized(this.init());

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
	public PreferencesFileSystem(String applicationName) throws IOException,
			SerializationException {
		super(applicationName);

		setInitialized(this.init());
	}

	/**
	 * Construction-only initialization.
	 * 
	 * @return true if successful initialization, otherwise false
	 * @see Preferences#init()
	 */
	private boolean init() {
		boolean inited = false;

		try {
			inited = createPreferencesPath();

			inited = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return inited;
	}

	/**
	 * Set the root path for all Preferences (in an implementation-dependent
	 * way).
	 * 
	 * @see Preferences
	 */
	@Override
	protected void setRootPath() {

		try {
			this.rootPath =
			// System.getProperty("user.home") + getPathSeparator() // ok
			System.getProperty("user.home").replaceAll("\\.",
					getPathSeparator())
					+ getPathSeparator() // better aligned with baseName
			;
		} catch (SecurityException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Set the base path for all Preferences
	 * 
	 * @see Preferences
	 */
	@Override
	protected void setBasePath(String basePath) {
		this.basePath = PIVOT_PREFS_DIR + getPathSeparator();
	}

	/**
	 * Returns the character to use as separator at the end of the Path part,
	 * and before the name.
	 */
	@Override
	protected String getPathSeparator() {
		return File.separator;
	}

	/**
	 * Set the base name for all Preferences of this application
	 * 
	 * @see Preferences
	 */
	@Override
	protected void setBaseName(String baseName) {
		// this.baseName = getApplicationName(); // flat name version
		this.baseName = getApplicationName().replaceAll("\\.", "/"); // tree
																		// name
																		// version
	}

	/**
	 * Utility method that recursively empties a directory, then delete it.
	 * 
	 * @param dir
	 *            the (main) directory to delete
	 * @return true if deleted, otherwise false
	 */
	protected static final boolean deleteDir(final File dir) {
		if (dir.exists() && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success)
					return false;

			}

		}

		return dir.delete();
	}

	/**
	 * Utility method that gives the dir contents, filtered by the given filter.
	 * 
	 * @param dir
	 *            the (main) directory to list
	 * @param filter
	 *            the filter to use (usually by extension)
	 * @return a File [] with all matched contents, but only at the root level
	 *         (no subdir)
	 */
	protected static final File[] listDir(final File dir, final String filter) {
		File[] children = null;
		if (dir.exists() && dir.isDirectory()) {
			children = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(filter);
				}
			});
		}

		return children;
	}

	/**
	 * Creates all the directory structure for the storage of all Preferences
	 * for the current application (using applicationName, given in the
	 * constructor)
	 * 
	 * @return true if successful
	 */
	@Override
	protected boolean createPreferencesPath() {
		boolean success = false;
		String prefsPath = getPreferencesPath();
		File f = new File(prefsPath);
		if (!f.exists())
			success = f.mkdirs();

		return success;
	}

	/**
	 * Delete all the directory structure for the storage of all Preferences for
	 * the current application (using applicationName, given in the
	 * constructor).
	 * <p>
	 * Attention, this is usually not needed by most applications, if not for
	 * cleanup operations.
	 * </p>
	 * 
	 * @return true if successful
	 */
	@Override
	protected boolean deletePreferencesPath() {
		// verify if proceed only if the baseName is not the default ...
		String prefsPath = getPreferencesPath();
		File prefsDir = new File(prefsPath);

		return PreferencesFileSystem.deleteDir(prefsDir);
	}

	@Override
	protected void writePreferences() throws IOException,
			SerializationException {
		String fileName = getContextResourceName();
		OutputStream out = null;

		if (getPrefsMap() == null) {
			// verify if in this case it's better to delete the related file ...
			throw new SerializationException("preferences map is null.");
		}

		try {
			out = new FileOutputStream(fileName);
			if (out == null) {
				throw new SerializationException(
						"outputStream is null, unable to write to  \""
								+ fileName + "\".");
			}

			getSerializer().writeObject(getPrefsMap(), out);

		} catch (Exception e) {
			e.printStackTrace();
			throw new SerializationException("unable to write: "
					+ e.getMessage(), e);
		} finally {
			if (out != null)
				out.close();
		}

	}

	@Override
	@SuppressWarnings("unchecked")
	protected Map<String, Object> readPreferences() throws IOException,
			SerializationException {
		String fileName = getContextResourceName();
		InputStream in = null;

		Map<String, Object> resourceMap = null;

		try {
			in = new FileInputStream(fileName);
			if (in == null) {
				throw new SerializationException(
						"inputStream is null, unable to read from \""
								+ fileName + "\".");
			}

			resourceMap = (Map<String, Object>) getSerializer().readObject(in);

		} catch (Exception e) {
			e.printStackTrace();
			throw new SerializationException("unable to read: "
					+ e.getMessage(), e);
		} finally {
			if (in != null)
				in.close();
		}

		return resourceMap;
	}

	@Override
	public boolean existPreferences() throws IOException,
			SerializationException {
		boolean success = false;

		String fileName = getContextResourceName();
		File f = null;

		try {
			f = new File(fileName);

			if (f.exists() && f.isFile() && f.canRead())
				success = true;

		} catch (Exception e) {
			e.printStackTrace();
			throw new SerializationException("unable to check for existence: "
					+ e.getMessage(), e);
		} finally {
		}

		return success;
	}

	@Override
	public boolean deletePreferences() throws IOException,
			SerializationException {
		boolean success = false;

		String fileName = getContextResourceName();
		File f = new File(fileName);
		if (f.exists() && f.isFile() && f.canWrite())
			success = f.delete();

		return success;
	}

	@Override
	public String dumpPreferences() throws IOException, SerializationException {
		String fileName = getContextResourceName();
		String dump = null;

		/*
		 * // ok, but reading and parsing the file InputStream in = new
		 * FileInputStream(fileName); if (in == null) { throw new
		 * SerializationException("inputStream is null, unable to read from \""
		 * + fileName + "\"."); }
		 * 
		 * Map<String, Object> testMap = readPreferences();
		 * 
		 * // JSONSerializer serializer = new JSONSerializer();
		 * ByteArrayOutputStream outputStream = null; try { outputStream = new
		 * ByteArrayOutputStream(); serializer.writeObject(testMap,
		 * outputStream); } finally { outputStream.close();
		 * 
		 * dump = new String(outputStream.toByteArray()); }
		 */
		// new, reading the file without parsing
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		if (in == null) {
			throw new SerializationException(
					"input file is null, unable to read from \"" + fileName
							+ "\".");
		}

		StringBuffer buf = new StringBuffer();
		String str;
		try {
			while ((str = in.readLine()) != null) {
				buf.append(str);
			}

		} finally {
			in.close();

			dump = buf.toString();
		}

		return dump;
	}

	@Override
	public List<String> listPreferencesContexts() throws IOException,
			SerializationException {
		List<String> list = null;
		String prefsPath = getPreferencesPath();
		File prefsDir = new File(prefsPath);
		File[] files = PreferencesFileSystem.listDir(prefsDir,
				PIVOT_PREFS_DEFAULT_EXT);
		if (files == null)
			return list;
		// else ...
		list = new ArrayList<String>();

		for (int i = 0; i < files.length; i++) {
			String name = files[i].getName();

			// remove file extension from the file found
			int lastDotIndex = name.lastIndexOf('.');

			list.add(name.substring(0, lastDotIndex));
		}

		return list;
	}

	@Override
	public String toString() {
		String parent = super.toString();

		return parent + " + " + CLASS_NAME + " : { " + " }";
	}

}
